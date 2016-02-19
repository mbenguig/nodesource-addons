package org.ow2.proactive.resourcemanager.nodesource.infrastructure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeInformation;
import org.objectweb.proactive.core.runtime.ProActiveRuntime;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.python.google.common.collect.Sets;


public class VMWareInfrastructureTest {

    private VMWareInfrastructure vmwareInfrastructure;

    @Mock
    private ConnectorIaasController connectorIaasController;
    @Mock
    private NodeSource nodeSource;
    @Mock
    private Node node;
    @Mock
    private ProActiveRuntime proActiveRuntime;
    @Mock
    private NodeInformation nodeInformation;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        vmwareInfrastructure = new VMWareInfrastructure();

    }

    @Test
    public void testInitialParamateres() {
        assertThat(vmwareInfrastructure.username, is(nullValue()));
        assertThat(vmwareInfrastructure.password, is(nullValue()));
        assertThat(vmwareInfrastructure.endpoint, is(nullValue()));
        assertThat(vmwareInfrastructure.ram, is(512));
        assertThat(vmwareInfrastructure.cores, is(1));
        assertThat(vmwareInfrastructure.vmUsername, is(nullValue()));
        assertThat(vmwareInfrastructure.vmPassword, is(nullValue()));
        assertThat(vmwareInfrastructure.rmHostname, is(not(nullValue())));
        assertThat(vmwareInfrastructure.connectorIaasURL,
                is("http://" + vmwareInfrastructure.rmHostname + ":8080/connector-iaas"));
        assertThat(vmwareInfrastructure.image, is(nullValue()));
        assertThat(vmwareInfrastructure.numberOfInstances, is(1));
        assertThat(vmwareInfrastructure.numberOfNodesPerInstance, is(1));
        if (System.getProperty("os.name").contains("Windows")) {
            assertThat(vmwareInfrastructure.downloadCommand,
                    is("powershell -command \"& { (New-Object Net.WebClient).DownloadFile('http://" +
                        vmwareInfrastructure.rmHostname + "/rest/node.jar', 'node.jar') }\""));
        } else {
            assertThat(vmwareInfrastructure.downloadCommand,
                    is("wget -nv http://" + vmwareInfrastructure.rmHostname + ":8080/rest/node.jar"));

        }
        assertThat(vmwareInfrastructure.additionalProperties, is("-Dproactive.useIPaddress=true"));

    }

    @Test
    public void testConfigure() {
        when(nodeSource.getName()).thenReturn("Node source Name");
        vmwareInfrastructure.nodeSource = nodeSource;

        vmwareInfrastructure.configure("username", "password", "endpoint", "test.activeeon.com",
                "http://localhost:8088/connector-iaas", "vmware-image", "1", "512", "vmUsername",
                "vmPassword", "2", "3", "wget -nv test.activeeon.com/rest/node.jar", "-Dnew=value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void tesConfigureNotEnoughParameters() {

        when(nodeSource.getName()).thenReturn("Node source Name");
        vmwareInfrastructure.nodeSource = nodeSource;

        vmwareInfrastructure.configure("username", "password", "endpoint", "test.activeeon.com",
                "http://localhost:8088/connector-iaas", "publicKeyName", "2", "3",
                "wget -nv test.activeeon.com/rest/node.jar", "-Dnew=value");
    }

    @Test
    public void testAcquireNode() {

        when(nodeSource.getName()).thenReturn("Node source Name");
        vmwareInfrastructure.nodeSource = nodeSource;

        vmwareInfrastructure.configure("username", "password", "endpoint", "test.activeeon.com",
                "http://localhost:8088/connector-iaas", "vmware-image", "512", "1", "vmUsername",
                "vmPassword", "1", "3", "wget -nv test.activeeon.com/rest/node.jar", "-Dnew=value");

        vmwareInfrastructure.connectorIaasController = connectorIaasController;

        vmwareInfrastructure.rmUrl = "http://test.activeeon.com";

        when(connectorIaasController.createInfrastructure("node_source_name", "username", "password",
                "endpoint", false)).thenReturn("node_source_name");

        when(connectorIaasController.createInstances("node_source_name", "node_source_name", "vmware-image",
                1, 1, 512)).thenReturn(Sets.newHashSet("123", "456"));

        vmwareInfrastructure.acquireNode();

        verify(connectorIaasController, times(1)).waitForConnectorIaasToBeUP();

        verify(connectorIaasController).createInfrastructure("node_source_name", "username", "password",
                "endpoint", false);

        verify(connectorIaasController).createInstances("node_source_name", "node_source_name",
                "vmware-image", 1, 1, 512);

        verify(connectorIaasController, times(2)).executeScript(anyString(), anyString(), anyList());
    }

    @Test
    public void testAcquireAllNodes() {
        testAcquireNode();
    }

    @Test
    public void testRemoveNode() throws ProActiveException, RMException {
        when(nodeSource.getName()).thenReturn("Node source Name");
        vmwareInfrastructure.nodeSource = nodeSource;

        vmwareInfrastructure.configure("username", "password", "endpoint", "test.activeeon.com",
                "http://localhost:8088/connector-iaas", "vmware-image", "1", "512", "vmUsername",
                "vmPassword", "2", "3", "wget -nv test.activeeon.com/rest/node.jar", "-Dnew=value");

        vmwareInfrastructure.connectorIaasController = connectorIaasController;

        when(node.getProperty(VMWareInfrastructure.INSTANCE_ID_NODE_PROPERTY)).thenReturn("123");

        when(node.getNodeInformation()).thenReturn(nodeInformation);

        when(node.getProActiveRuntime()).thenReturn(proActiveRuntime);

        when(nodeInformation.getName()).thenReturn("nodename");

        vmwareInfrastructure.nodesPerInstances.put("123", Sets.newHashSet("nodename"));

        vmwareInfrastructure.removeNode(node);

        verify(proActiveRuntime).killNode("nodename");

        verify(connectorIaasController).terminateInstance("node_source_name", "123");

        assertThat(vmwareInfrastructure.nodesPerInstances.isEmpty(), is(true));

    }

    @Test
    public void testNotifyAcquiredNode() throws ProActiveException, RMException {

        when(nodeSource.getName()).thenReturn("Node source Name");
        vmwareInfrastructure.nodeSource = nodeSource;
        vmwareInfrastructure.configure("username", "password", "endpoint", "test.activeeon.com",
                "http://localhost:8088/connector-iaas", "vmware-image", "1", "512", "vmUsername",
                "vmPassword", "2", "3", "wget -nv test.activeeon.com/rest/node.jar", "-Dnew=value");

        vmwareInfrastructure.connectorIaasController = connectorIaasController;

        when(node.getProperty(VMWareInfrastructure.INSTANCE_ID_NODE_PROPERTY)).thenReturn("123");

        when(node.getNodeInformation()).thenReturn(nodeInformation);

        when(nodeInformation.getName()).thenReturn("nodename");

        vmwareInfrastructure.notifyAcquiredNode(node);

        assertThat(vmwareInfrastructure.nodesPerInstances.get("123").isEmpty(), is(false));
        assertThat(vmwareInfrastructure.nodesPerInstances.get("123").size(), is(1));
        assertThat(vmwareInfrastructure.nodesPerInstances.get("123").contains("nodename"), is(true));

    }

    @Test
    public void testGetDescription() {
        assertThat(vmwareInfrastructure.getDescription(),
                is("Handles nodes from the Amazon Elastic Compute Cloud Service."));
    }

}
