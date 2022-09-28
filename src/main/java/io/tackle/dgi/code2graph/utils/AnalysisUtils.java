package io.tackle.dgi.code2graph.utils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;

public class AnalysisUtils {

  /**
   * Verfy if a class is an application class.
   * 
   * @param _class
   * @return Boolean
   */
  private static Boolean _isApplicationClass(IClass _class) {
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
    int entrypointsCount=0;
    for (IClass c : cha) {
      if (_isApplicationClass(c)) {
        ShrikeClass sc = (ShrikeClass) c;
        String className = sc.getModuleEntry().getClassName();
        Log.info("Adding " + className + " to entrypoints.");
        try{
          for (IMethod method : c.getDeclaredMethods()) {
            Log.done("Added: " + method.getSignature());
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
