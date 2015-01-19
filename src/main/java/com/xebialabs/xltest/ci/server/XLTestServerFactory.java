/**
 * Copyright (c) 2014-2015, XebiaLabs B.V., All rights reserved.
 *
 * The XL Test plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs
 * Libraries. There are special exceptions to the terms and conditions of the
 * GPLv2 as it is applied to this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xltest-plugin/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.xebialabs.xltest.ci.server;

import java.util.List;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;


public class XLTestServerFactory {
    public boolean validConnection(String serverUrl, String proxyUrl, String username, String password) throws IllegalStateException {
        newInstance(serverUrl, proxyUrl, username, password).newCommunicator();  //throws IllegalStateException on failure.
        return true;
    }


    public static XLTestServer newInstance(String serverUrl, String proxyUrl, String username, String password) {
        XLTestServerImpl server = new XLTestServerImpl(serverUrl, proxyUrl, username, password);
        return Reflection.newProxy(XLTestServer.class, new PluginFirstClassloaderInvocationHandler(server));
    }


    public static String getNameFromId(String id) {
        String[] nameParts = id.split("/");
        return nameParts[nameParts.length - 1];
    }

    public static String getParentId(String id) {
        String[] nameParts = id.split("/");
        List<String> list = Lists.newArrayList(nameParts);
        if (list.size() > 1) {
            list.remove(nameParts.length - 1);
        }
        return Joiner.on("/").join(list);
    }
}