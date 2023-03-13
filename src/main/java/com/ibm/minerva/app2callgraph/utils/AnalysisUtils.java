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

package com.ibm.minerva.app2callgraph.utils;

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
   * Verify if a class is an application class.
   *
   * @param _class : The given class that needs to be verified as an application
   *               class.
   * @return Boolean : Returns true if the input class is an application class;
   *         false otherwise.
   */
  public static Boolean isApplicationClass(IClass _class) {
    if (_class.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Use all public methods of all application classes as entry points.
   *
   * @param cha : The class hierarchy object that holds all the classes.
   * @return Iterable<Entrypoint> : An iterable collection of entry points (public
   *         methods of all application classes).
   */
  public static Iterable<Entrypoint> getEntryPoints(IClassHierarchy cha) {
    Collection<Entrypoint> entrypoints = new ArrayList<>();
    int entrypointsCount = 0;
    // Iterates over all classes of the hierarchy and adds all public methods of
    // application classes as entrypoints.
    for (IClass c : cha) {
      if (isApplicationClass(c)) { // Checks whether the class is an application class.
        try {
          for (IMethod method : c.getDeclaredMethods()) { // Loop through all methods available in the given class.
            if (method.isPublic()) { // Check if method is public.
              entrypointsCount += 1; // Increment the count of entry points found.
              entrypoints.add(new DefaultEntrypoint(method, cha)); // Add the public method to the entry point
                                                                   // collection.
            }
          }
        } catch (NullPointerException nullPointerException) { // Handle any possible exception.
          Log.error(c.getSourceFileName());
          System.exit(1);
        }
      }
    }
    Log.info("Registered " + entrypointsCount + " entrypoints."); // Print the total number of entry points found.
    return entrypoints; // Return the iterable entry points.
  }
}
