/*
 * Copyright 2022 Kerbaya Software
 * 
 * This file is part of stamp-maven-plugin. 
 * 
 * stamp-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stamp-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stamp-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.stamp;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * creates an uncached repo session
 */
class NonCachingSessionFactory implements AutoCloseable, Supplier<RepositorySystemSession>
{
	private final RepositorySystem rs;
	private final RepositorySystemSession rss;
	private final Path localRepoPath;
	
	public NonCachingSessionFactory(RepositorySystem rs, RepositorySystemSession rss)
	{
		this.rs = rs;
		this.rss = rss;
		try
		{
			localRepoPath = Files.createTempDirectory(null);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	private static void tryDeletePath(Path path)
	{
		if (Files.isDirectory(path))
		{
			tryDeleteDir(path);
		}
		else
		{
			try
			{
				Files.delete(path);
			}
			catch (IOException e)
			{
			}
		}
	}
	
	private static void tryDeleteDir(Path path)
	{
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(path))
		{
			ds.forEach(NonCachingSessionFactory::tryDeletePath);
		}
		catch (IOException e)
		{
		}
		
		try
		{
			Files.delete(path);
		}
		catch (IOException e)
		{
		}
	}
	
	@Override
	public void close()
	{
		tryDeleteDir(localRepoPath);
	}
	
	@Override
	public RepositorySystemSession get()
	{
		return new DefaultRepositorySystemSession(rss)
				.setWorkspaceReader(null)
				.setCache(null)
				.setIgnoreArtifactDescriptorRepositories(true)
				.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS)
				.setLocalRepositoryManager(rs.newLocalRepositoryManager(
						rss, new LocalRepository(localRepoPath.toFile())));
	}
}
