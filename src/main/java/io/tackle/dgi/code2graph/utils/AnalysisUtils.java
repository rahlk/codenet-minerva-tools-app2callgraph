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

package io.tackle.dgi.code2graph.utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import java.util.ArrayList;
import java.util.Collection;

public class AnalysisUtils {

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
}
