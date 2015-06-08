/**
 * Copyright (c) 2014-2015, XebiaLabs B.V., All rights reserved.
 * <p/>
 * The XL TestView plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs
 * Libraries. There are special exceptions to the terms and conditions of the
 * GPLv2 as it is applied to this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xltestview-plugin/blob/master/LICENSE>.
 * <p/>
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; version 2 of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.xebialabs.xltest.ci.server;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.xebialabs.xltest.ci.server.authentication.UsernamePassword;

public class XLTestServerFactory {

    /* wrap the StandardUsernamePasswordCredentials so we can test the XLTestServerImpl
      Jenkins' Secret class is too final and too tightly coupled to a running Jenkins.
     */
    static final class UsernamePasswordImpl implements UsernamePassword {
        private final StandardUsernamePasswordCredentials credentials;

        public UsernamePasswordImpl(StandardUsernamePasswordCredentials credentials) {
            this.credentials = credentials;
        }

        @Override
        public String getUsername() {
            return credentials.getUsername();
        }

        @Override
        public String getPassword() {
            return credentials.getPassword().getPlainText();
        }
    }

    public static XLTestServer newInstance(String serverUrl, String proxyUrl, StandardUsernamePasswordCredentials credentials) {
        return new XLTestServerImpl(serverUrl, proxyUrl, new UsernamePasswordImpl(credentials));
    }
}
