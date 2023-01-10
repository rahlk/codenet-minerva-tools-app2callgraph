/*
Copyright IBM Corporation 2022

Licensed under the Apache Public License 2.0, Version 2.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.konveyor.dgi.code2graph.utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnalysisUtils {

  public static Map<String, Map<String, Object>> classAttr = new HashMap<String, Map<String, Object>>();

  /**
   * Verfy if a class is an application class.
   *
   * @param _class
   * @return Boolean
   */
  public static Boolean isApplicationClass(IClass _class) {
    if (_class.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Use all public methods of all application classes as entrypoints.
   *
   * @param cha
   * @return Iterable<Entrypoint>
   */
  public static Iterable<Entrypoint> getEntryPoints(IClassHierarchy cha) {
    Collection<Entrypoint> entrypoints = new ArrayList<>();
    int entrypointsCount = 0;
    for (IClass c : cha) {
      if (isApplicationClass(c)) {
        try {
          for (IMethod method : c.getDeclaredMethods()) {
            if (method.isPublic()) {
              entrypointsCount += 1;
              entrypoints.add(new DefaultEntrypoint(method, cha));
            }
          }
        } catch (NullPointerException nullPointerException) {
          Log.error(c.getSourceFileName());
          System.exit(1);
        }
      }
    }
    Log.info("Registered " + entrypointsCount + " entrypoints.");
    return entrypoints;
  }

  public static void expandSymbolTable(IClassHierarchy cha) {
    for (IClass c : cha) {
      if (isApplicationClass(c)) {
        // private constructor
        String className = c.getName().getClassName().toString();
        Map<String, Object> classAttributeMap = new HashMap<String, Object>();
        String classIsPrivate = Boolean.toString(c.isPrivate());
        classAttributeMap.put("isPrivate", classIsPrivate);
        String classSourcePath = c.getSourceFileName();
        classAttributeMap.put("source_file_name", classSourcePath);
        Integer num_fields = c.getAllFields().size();
        classAttributeMap.put("num_fields", num_fields);
        Integer num_static_fields = c.getAllStaticFields().size();
        classAttributeMap.put("num_static_fields", num_static_fields);
        Integer num_static_methods = 0;
        for (IMethod method : c.getDeclaredMethods()) {
          num_static_methods += method.isStatic() ? 1 : 0;
        }
        classAttributeMap.put("num_static_methods", num_static_methods);
        classAttributeMap.put("num_declared_methods", c.getDeclaredMethods().size());
        classAttr.put(className, classAttributeMap);
      }
    }
  }
}
