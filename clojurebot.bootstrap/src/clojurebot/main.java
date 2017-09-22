/* Copyright © Paul Stadig.  All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as defined
 * by the Mozilla Public License, v. 2.0.
*/
package clojurebot;

import java.net.URL;
import java.net.URLClassLoader;
import org.projectodd.shimdandy.ClojureRuntimeShim;

public class main {
  private static final String clojurebotCoreJarPath =
    System.getenv("CLOJUREBOT_CORE_PATH");

  public static void main(String[] args) throws Exception {
    if (!(new java.io.File(clojurebotCoreJarPath)).exists()) {
      throw new java.io.IOException(clojurebotCoreJarPath + " does not exist");
    }
    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
    URL clojurebotCoreJar = new URL("file:" + clojurebotCoreJarPath);
    ClassLoader loader = new URLClassLoader(new URL[] { clojurebotCoreJar });
    ClojureRuntimeShim shim = ClojureRuntimeShim.newRuntime(loader,
                                                            "clojurebot");
    shim.require("clojurebot.core");
    shim.invoke("clojurebot.core/main");
  }
}
