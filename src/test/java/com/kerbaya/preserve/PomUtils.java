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

import java.io.File;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.xml.sax.InputSource;

public class PomUtils
{
	private static final String POM_NS_URI = "http://maven.apache.org/POM/4.0.0";
	private static final String POM_NS_PREFIX = "pom";
	
	private static final NamespaceContext NS_CTX = new NamespaceContext() {
		@Override
		public Iterator<String> getPrefixes(String namespaceURI)
		{
			return Collections.singleton(getPrefix(namespaceURI)).iterator();
		}
		
		@Override
		public String getPrefix(String namespaceURI)
		{
			switch (namespaceURI)
			{
			case POM_NS_URI:
				return POM_NS_PREFIX;
			case XMLConstants.XML_NS_URI:
				return XMLConstants.XML_NS_PREFIX;
			case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
				return XMLConstants.XMLNS_ATTRIBUTE;
			}
			return null;
		}
		
		@Override
		public String getNamespaceURI(String prefix)
		{
			switch (prefix)
			{
			case POM_NS_PREFIX:
				return POM_NS_URI;
			case XMLConstants.XMLNS_ATTRIBUTE:
				return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
			case XMLConstants.XML_NS_PREFIX:
				return XMLConstants.XML_NS_URI;
			}
			return null;
		}
	};
	
	public static String getString(String xpath, File baseDir, String... pomPath)
	{
		Path pomFolder = baseDir.toPath();
		for (String pathEntry: pomPath)
		{
			pomFolder = pomFolder.resolve(pathEntry);
		}
		
		InputSource pom = new InputSource(pomFolder.resolve("pom.xml").toUri().toString());
		XPath xp = XPathFactory.newInstance().newXPath();
		xp.setNamespaceContext(NS_CTX);
		
		try
		{
			return xp.evaluate(xpath, pom);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	public static void match(String expectedRegex, String xpath, File baseDir, String... pomPath)
	{
		String str = getString(xpath, baseDir, pomPath);
		Assert.assertTrue(String.format("%s != %s", expectedRegex, str), Pattern.matches(expectedRegex, str));
	}
	
	private static String getVersion(File baseDir, String... pomPath)
	{
		return getString("/pom:project/pom:version[text()]", baseDir, pomPath);
	}
	
	public static void assertVersion(String version, File baseDir, String... pomPath)
	{
		Assert.assertEquals(version, getVersion(baseDir, pomPath));
	}
	
	public static void assertNoVersion(File baseDir, String... pomPath)
	{
		Assert.assertNull(getVersion(baseDir, pomPath));
	}
	
	private static void assertNumbers(CharBuffer cb, int count)
	{
		for (int i = 0; i < count; i++)
		{
			assertCharRange(cb, '0', '9');
		}
	}
	
	private static void assertChar(CharBuffer cb, char c)
	{
		Assert.assertTrue(cb.hasRemaining());
		Assert.assertEquals(c, cb.get());
	}
	
	private static void assertCharRange(CharBuffer cb, char first, char last)
	{
		Assert.assertTrue(cb.hasRemaining());
		char c = cb.get();
		Assert.assertTrue(c >= first && c <= last);
	}
	
	private static void assertStartsWith(String expected, CharBuffer actual)
	{
		CharBuffer expectedCb = CharBuffer.wrap(expected);
		while (expectedCb.hasRemaining())
		{
			Assert.assertTrue(actual.hasRemaining());
			Assert.assertEquals(expectedCb.get(), actual.get());
		}
	}
	
	public static void assertBuildNumber(
			String expectedPrefix, long expectedBuildNumber, File baseDir, String... pomPath)
	{
		CharBuffer versionCb = CharBuffer.wrap(getVersion(baseDir, pomPath));
		assertStartsWith(expectedPrefix, versionCb);
		
		assertChar(versionCb, '-');
		assertNumbers(versionCb, 8);
		assertChar(versionCb, '.');
		assertNumbers(versionCb, 6);
		assertChar(versionCb, '-');

		Assert.assertEquals(Long.toString(expectedBuildNumber), versionCb.toString());
	}
}
