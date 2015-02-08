// Copyright 2006, 2007 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.anjlab.eclipse.tapestry5.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;

/**
 * Used to order objects into an "execution" order. Each object must have a
 * unique id. It may specify a list of constraints which identify the ordering
 * of the objects.
 */
public class IdToDependencyNode<T>
{
    private final OneShotLock lock = new OneShotLock();

    private final List<Orderable<T>> orderables = new ArrayList<Orderable<T>>();

    private final Map<String, Orderable<T>> idToOrderable = new CaseInsensitiveMap<Orderable<T>>();

    private final Map<String, DependencyNode<T>> idToDependencyNode = new CaseInsensitiveMap<DependencyNode<T>>();

    // Special node that is always dead last: all other nodes are a dependency
    // of the trailer.

    private DependencyNode<T> trailer;

    interface DependencyLinker<T>
    {
        void link(DependencyNode<T> source, DependencyNode<T> target);
    }

    // before: source is added as a dependency of target, so source will
    // appear before target.

    final DependencyLinker<T> before = new DependencyLinker<T>()
    {
        public void link(DependencyNode<T> source, DependencyNode<T> target)
        {
            target.addDependency(source);
        }
    };

    // after: target is added as a dependency of source, so source will appear
    // after target.

    final DependencyLinker<T> after = new DependencyLinker<T>()
    {
        public void link(DependencyNode<T> source, DependencyNode<T> target)
        {
            source.addDependency(target);
        }
    };

    /**
     * Adds an object to be ordered.
     *
     * @param orderable
     */
    public void add(Orderable<T> orderable)
    {
        lock.check();

        String id = orderable.getId();

        if (idToOrderable.containsKey(id))
        {
            return;
        }

        orderables.add(orderable);

        idToOrderable.put(id, orderable);
    }

    /**
     * Adds an object to be ordered.
     *
     * @param id
     *            unique, qualified id for the target
     * @param target
     *            the object to be ordered (or null as a placeholder)
     * @param constraints
     *            optional, variable constraints
     * @see #add(org.apache.tapestry5.ioc.Orderable)
     */

    public void add(String id, T target, String... constraints)
    {
        lock.check();

        add(new Orderable<T>(id, target, constraints));
    }

    public List<T> getOrdered()
    {
        lock.lock();

        initializeGraph();

        List<T> result = new ArrayList<T>();

        for (Orderable<T> orderable : trailer.getOrdered())
        {
            T target = orderable.getTarget();

            // Nulls are placeholders that are skipped.

            if (target != null)
                result.add(target);
        }

        return result;
    }

    private void initializeGraph()
    {
        trailer = new DependencyNode<T>(new Orderable<T>("*-trailer-*", null));

        addNodes();

        addDependencies();
    }

    private void addNodes()
    {
        for (Orderable<T> orderable : orderables)
        {
            DependencyNode<T> node = new DependencyNode<T>(orderable);

            idToDependencyNode.put(orderable.getId(), node);

            trailer.addDependency(node);
        }
    }

    private void addDependencies()
    {
        for (Orderable<T> orderable : orderables)
        {
            addDependencies(orderable);
        }
    }

    private void addDependencies(Orderable<T> orderable)
    {
        String sourceId = orderable.getId();

        for (String constraint : orderable.getConstraints())
        {
            addDependencies(sourceId, constraint);
        }
    }

    private void addDependencies(String sourceId, String constraint)
    {
        int colonx = constraint.indexOf(':');

        String type = colonx > 0 ? constraint.substring(0, colonx) : null;

        DependencyLinker<T> linker = null;

        if ("after".equals(type))
            linker = after;
        else if ("before".equals(type))
            linker = before;

        if (linker == null)
        {
            return;
        }

        String patternList = constraint.substring(colonx + 1);

        linkNodes(sourceId, patternList, linker);
    }

    private void linkNodes(String sourceId, String patternList,
            DependencyLinker<T> linker)
    {
        Collection<DependencyNode<T>> nodes = findDependencies(sourceId,
                patternList);

        DependencyNode<T> source = idToDependencyNode.get(sourceId);

        for (DependencyNode<T> target : nodes)
        {
            linker.link(source, target);
        }
    }

    private Collection<DependencyNode<T>> findDependencies(String sourceId,
            String patternList)
    {
        Matcher matcher = buildMatcherForPattern(patternList);

        Collection<DependencyNode<T>> result = new ArrayList<DependencyNode<T>>();

        for (String id : idToDependencyNode.keySet())
        {
            if (sourceId.equals(id))
                continue;

            if (matcher.matches(
                    new TapestryService(null, new ServiceDefinition().setId(id), null)))
            {
                result.add(idToDependencyNode.get(id));
            }
        }

        return result;
    }

    private Matcher buildMatcherForPattern(String patternList)
    {
        List<Matcher> matchers = new ArrayList<Matcher>();

        for (String pattern : patternList.split(","))
        {
            Matcher matcher = new IdentityIdMatcher(pattern.trim());

            matchers.add(matcher);
        }

        return matchers.size() == 1 ? matchers.get(0) : new OrMatcher(matchers);
    }
}
