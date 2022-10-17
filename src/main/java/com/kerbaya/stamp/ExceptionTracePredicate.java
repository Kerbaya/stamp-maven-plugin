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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

class ExceptionTracePredicate implements Predicate<Throwable>
{
	private final List<Class<? extends Throwable>> trace;
	
	@SafeVarargs
	public ExceptionTracePredicate(Class<? extends Throwable>... trace)
	{
		this.trace = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(trace)));
	}
	
	private static List<Class<? extends Throwable>> createTrace(Throwable t)
	{
		if (t == null)
		{
			return Collections.emptyList();
		}
		
		List<Class<? extends Throwable>> trace = new ArrayList<>();
		Set<Throwable> deja = new HashSet<>();
		
		do
		{
			if (!deja.add(t))
			{
				return null;
			}
			
			trace.add(t.getClass());
		} while ((t = t.getCause()) != null);
		
		return trace;
	}
	
	@Override
	public boolean test(Throwable t)
	{
		return trace.equals(createTrace(t));
	}
}
