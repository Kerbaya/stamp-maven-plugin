/*
 * Copyright 2022 Kerbaya Software
 * 
 * This file is part of preserve-maven-plugin. 
 * 
 * preserve-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * preserve-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with preserve-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.preserve;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.MetadataNotFoundException;

import lombok.Getter;
import lombok.Setter;

@org.apache.maven.plugins.annotations.Mojo(
		name="stamp",
		requiresProject=true,
		requiresDirectInvocation=true,
		threadSafe=true)
public class StampMojo implements org.apache.maven.plugin.Mojo
{
	private static final String ALT_REPO_TOKEN_SEPARATOR = "::";
	
	private static final long FIRST_BUILD_NUMBER = 1L;
	private static final long BUILD_NUMBER_INCREMENT = 1L;
	
	private static Predicate<? super Throwable> ARTIFACT_MISSING = 
			new ExceptionTracePredicate(MetadataNotFoundException.class);
	
	private static final DateTimeFormatter SNAPSHOT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")
			.withZone(ZoneOffset.UTC);
	
	private static final String SET_VERSION_GOAL = "org.codehaus.mojo:versions-maven-plugin:2.12.0:set";
	
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	
	@Getter
	@Setter
	private Log log;
	
	@Inject
	private MavenProject rootProject;
	
	@Inject
	private Invoker invoker;
	
	@Inject
	private RepositorySystem rs;

	@Parameter(defaultValue="${repositorySystemSession}", required=true, readonly=true)
	private RepositorySystemSession rss;
	
    @Parameter(property="altDeploymentRepository")
    private String altDeploymentRepository;

    @Parameter(property="altSnapshotDeploymentRepository")
    private String altSnapshotDeploymentRepository;
    
    protected RemoteRepository getRepo(String id, String url)
    {
        RemoteRepository remoteRepo = new RemoteRepository.Builder(id, "default", url).build();
        
        boolean hasAuthentication = remoteRepo.getAuthentication() != null;
        boolean hasProxy = remoteRepo.getProxy() != null;
        
        if (hasAuthentication && hasProxy)
        {
        	return remoteRepo;
        }
        
        RemoteRepository.Builder builder = new RemoteRepository.Builder(remoteRepo);
        if (!hasAuthentication)
        {
        	builder.setAuthentication(rss.getAuthenticationSelector().getAuthentication(remoteRepo));
        }
        
        if (!hasProxy)
        {
        	builder.setProxy(rss.getProxySelector().getProxy(remoteRepo));
        }
        
        return builder.build();
    }
    
    private RemoteRepository getAltSnapshotRepo()
    {
    	String altRepo;
    	if ((altRepo = altSnapshotDeploymentRepository) == null)
    	{
    		if ((altRepo = altDeploymentRepository) == null)
    		{
    			return null;
    		}
    	}
    	
    	int sepIdx = altRepo.indexOf(ALT_REPO_TOKEN_SEPARATOR);
    	if (sepIdx != -1)
    	{
        	String id = altRepo.substring(0, sepIdx);
        	String url = altRepo.substring(sepIdx + ALT_REPO_TOKEN_SEPARATOR.length());

        	if (!id.isEmpty() && !url.isEmpty())
        	{
        		return getRepo(id, url);
        	}
    	}
    	
    	throw new IllegalStateException("invalid alt repo syntax: " + altRepo);
    }
    
    private RemoteRepository getSnapshotDistRepo(MavenProject project)
    {
    	RemoteRepository altRepo = getAltSnapshotRepo();
    	if (altRepo != null)
    	{
    		return altRepo;
    	}
    	
    	DistributionManagement dm = project.getDistributionManagement();
    	if (dm == null)
    	{
    		return null;
    	}
    	
		DeploymentRepository modelRepo = dm.getSnapshotRepository();
		if (modelRepo == null)
		{
			modelRepo = dm.getRepository();
			if (modelRepo == null)
			{
				return null;
			}
		}

		if (!modelRepo.isUniqueVersion())
		{
			return null;
		}
		
		return getRepo(modelRepo.getId(), modelRepo.getUrl());
    }
    
	private void debug(String pattern, Object... args)
	{
		if (log != null && log.isDebugEnabled())
		{
			log.debug(args.length == 0 ? pattern : String.format(pattern, args));
		}
	}
	
	private static void throwOnResolveError(Throwable ex)
	{
		if (!ARTIFACT_MISSING.test(ex))
		{
			throw new IllegalStateException("version resolve error", ex);
		}
	}
	
	private static void throwOnResolveError(VersionResult verRes)
	{
		verRes.getExceptions().forEach(StampMojo::throwOnResolveError);
	}
	
	private static String stripSnapshot(String snapshotVersion)
	{
		return snapshotVersion.substring(0, snapshotVersion.length() - SNAPSHOT_SUFFIX.length());

	}
	
	private Long getNextBuildNumber(MavenProject project, String snapshotVersion)
	{
		RemoteRepository snapshotRepo = getSnapshotDistRepo(project);
		
		if (snapshotRepo == null)
		{
			throw new IllegalStateException("no distribution repo");
		}
		
		VersionRequest verReq = new VersionRequest(
				new DefaultArtifact(project.getGroupId(), project.getArtifactId(), "pom", snapshotVersion),
				Collections.singletonList(snapshotRepo),
				null);
		
		VersionResult verRes;
		
		RepositorySystemSession nonCachedRss = new DefaultRepositorySystemSession(rss)
				.setWorkspaceReader(null);
//				.setConfigProperty("aether.versionResolver.noCache", true);
		try
		{
			verRes = rs.resolveVersion(nonCachedRss, verReq);
		}
		catch (VersionResolutionException e)
		{
			verRes = e.getResult();
			if (verRes == null)
			{
				throwOnResolveError(e.getCause());
				return FIRST_BUILD_NUMBER;
			}
		}
		
		String lastVersion = verRes.getVersion();
		
		if (lastVersion == null)
		{
			throwOnResolveError(verRes);
			return FIRST_BUILD_NUMBER;
		}
		
		if (lastVersion.equals(snapshotVersion))
		{
			List<Exception> exList = verRes.getExceptions();
			if (exList.isEmpty())
			{
				return null;
			}
			
			throwOnResolveError(verRes);
			return FIRST_BUILD_NUMBER;
		}
		
		Pattern p = Pattern.compile(Pattern.quote(stripSnapshot(snapshotVersion)) + "-\\d{8}\\.\\d{6}-(\\d+)");
		Matcher m = p.matcher(lastVersion);
		if (!m.matches())
		{
			throw new IllegalStateException("invalid lastVersion: " + lastVersion);
		}
		
		return Long.parseLong(m.group(1)) + BUILD_NUMBER_INCREMENT;
	}
	
	private String getNextVersion(MavenProject project, Instant now)
	{
		String version = project.getVersion();
		if (version == null || !version.endsWith(SNAPSHOT_SUFFIX))
		{
			debug("not a snapshot");
			return null;
		}
		
		Long nextBuildNumber = getNextBuildNumber(project, version);
		
		if (nextBuildNumber == null)
		{
			debug("repo not using build numbers");
			return null;
		}
		
		return stripSnapshot(version) + "-" + SNAPSHOT_TIME_FORMAT.format(now) + "-" + nextBuildNumber;
	}
	
	private void execute0() throws MavenInvocationException, CommandLineException, VersionResolutionException
	{
		if (rootProject.getOriginalModel().getVersion() == null)
		{
			debug("inherited version");
			return;
		}
		
		Instant now = Instant.now();
		
		String nextVersion = getNextVersion(rootProject, now);
		if (nextVersion == null)
		{
			debug("no next version");
			return;
		}
		
		debug("setting new version");
		Properties p = new Properties();
		p.setProperty("newVersion", nextVersion);
		p.setProperty("generateBackupPoms", "false");
		p.setProperty("processAllModules", "true");
		InvocationRequest ir = new DefaultInvocationRequest()
				.setBatchMode(true)
				.setNoTransferProgress(true)
				.setDebug(log != null && log.isDebugEnabled())
				.setPomFile(rootProject.getFile())
				.setGoals(Collections.singletonList(SET_VERSION_GOAL))
				.setProperties(p);
		InvocationResult res = invoker.execute(ir);
		CommandLineException ex = res.getExecutionException();
		if (ex != null)
		{
			throw ex;
		}
		
		int rc = res.getExitCode();
		
		if (rc != 0)
		{
			throw new IllegalStateException("set failed: " + rc);
		}
	}
	
	@Override
	public void execute() throws MojoExecutionException
	{
		try
		{
			execute0();
		}
		catch (MavenInvocationException | CommandLineException | VersionResolutionException e)
		{
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
