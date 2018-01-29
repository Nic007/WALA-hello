/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nicolas Cloutier - Polymtl Modification for an easy build
 *******************************************************************************/
package com.polymtl.hello.drivers;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Collection;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

/**
 *
 * This simple example WALA application analyze a project and push it.
 *
 * @author Nicolas Cloutier
 */
public class DumpWala extends BasicAnalysis {
  public void run() throws IOException {
    try {
      // invoke WALA to build a class hierarchy
      ClassHierarchy cha = ClassHierarchyFactory.make(this.scope);
      Graph<IClass> g = typeHierarchy2Graph(cha);
      g = pruneForAppLoader(g);

      // Dump graph to file
      String outFile = File.createTempFile("out", ".txt").getAbsolutePath();
      System.out.println(outFile);
      try(PrintWriter out = new PrintWriter(outFile)) {
          out.println(g);
      }

      return;
    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }
  }

  public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) throws WalaException {
    Collection<T> slice = GraphSlicer.slice(g, f);
    return GraphSlicer.prune(g, new CollectionFilter<>(slice));
  }

  /**
   * Restrict g to nodes from the Application loader
   */
  public static Graph<IClass> pruneForAppLoader(Graph<IClass> g) throws WalaException {
    Predicate<IClass> f = new Predicate<IClass>() {
      @Override public boolean test(IClass c) {
        return (c.getClassLoader().getReference().equals(ClassLoaderReference.Application));
      }
    };
    return pruneGraph(g, f);
  }

  /**
   * Return a view of an {@link IClassHierarchy} as a {@link Graph}, with edges from classes to immediate subtypes
   */
  public static Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) throws WalaException {
    Graph<IClass> result = SlowSparseNumberedGraph.make();
    for (IClass c : cha) {
      result.addNode(c);
    }
    for (IClass c : cha) {
      for (IClass x : cha.getImmediateSubclasses(c)) {
        result.addEdge(c, x);
      }
      if (c.isInterface()) {
        for (IClass x : cha.getImplementors(c.getReference())) {
          result.addEdge(c, x);
        }
      }
    }
    return result;
  }
}
