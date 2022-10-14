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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class StampIT
{
	private static final Pattern BUILD_NUMBER_PATTERN = 
			Pattern.compile("<version>1\\.0\\.0-\\d{8}\\.\\d{6}-(\\d+)</version>");
	
	private static void assertBuildNumber(long expected, String itFolder)
	{
		Path path = Paths.get("target", "its", "stamp", itFolder, "pom.xml");
		byte[] bytes;
		try
		{
			bytes = Files.readAllBytes(path);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
		Matcher m = BUILD_NUMBER_PATTERN.matcher(new String(bytes, StandardCharsets.UTF_8));
		Assert.assertTrue(m.find());
		Assert.assertEquals(expected, Long.parseLong(m.group(1)));
	}
	
	@Test
	public void noPrevious()
	{
		assertBuildNumber(1L, "no-previous");
	}
	
	@Test
	public void onePrevious()
	{
		assertBuildNumber(2L, "one-previous");
	}
	
	@Test
	public void twoPrevious()
	{
		assertBuildNumber(3L, "two-previous");
	}
}
