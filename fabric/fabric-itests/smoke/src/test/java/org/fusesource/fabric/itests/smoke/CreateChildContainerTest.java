/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.itests.smoke;

import junit.framework.Assert;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;


import java.util.Set;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CreateChildContainerTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testLocalChildCreation() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.child(1).withName("child").assertProvisioningResult().build();
    }

    /**
     * This is a test for: http://fusesource.com/issues/browse/FABRIC-370
     *
     * @throws Exception
     */
    @Test
    public void testContainerDelete() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("fabric:version-create"));
        Set<Container> containers = ContainerBuilder.child(1).withName("child").assertProvisioningResult().build();
        CuratorFramework curator = getOsgiService(CuratorFramework.class);
        for (Container c : containers) {
            try {
                c.destroy();
                Assert.assertNull(exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.1", c.getId())));
                Assert.assertNull(exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.0", c.getId())));
                Assert.assertNull(exists(curator, ZkPath.CONTAINER.getPath(c.getId())));
                Assert.assertNull(exists(curator, ZkPath.CONTAINER_DOMAINS.getPath(c.getId())));
                Assert.assertNull(exists(curator, ZkPath.CONTAINER_PROVISION.getPath(c.getId())));
            } catch (Exception ex) {
                //ignore
            }
        }
    }

    /**
     * http://fusesource.com/issues/browse/FABRIC-351
     *
     * @throws Exception
     */
    @Test
    public void testContainerWithJvmOpts() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.child(1).withName("child").
                withJvmOpts("-Xms512m -XX:MaxPermSize=512m -Xmx2048m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5008")
                .assertProvisioningResult().build();
    }

    /**
     * This is a test for regressions after adding:
     * https://fusesource.com/issues/browse/FABRIC-482
     * Even though the issue is specific to ssh containers the same principals apply to child.
     * @throws Exception
     */
    @Test
    public void testContainerWithPasswordChange() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.child(1).withName("child").assertProvisioningResult().build();
        Thread.sleep(5000);
        Container container = containers.iterator().next();
        System.err.println(
                executeCommands(
                        "jaas:manage --realm karaf --module org.fusesource.fabric.jaas.ZookeeperLoginModule",
                        "jaas:userdel admin",
                        "jaas:useradd admin newpassword",
                        "jaas:roleadd admin admin",
                        "jaas:update"

                )
        );
        System.err.println(executeCommand("fabric:container-stop --user admin --password newpassword "+container.getId()));
        Provision.containersAlive(containers, false, 6 * DEFAULT_TIMEOUT);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                debugConfiguration("5005",false)
        };
    }
}
