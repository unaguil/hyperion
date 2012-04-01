package dissemination.newProtocol.ptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class ParameterTableTest {
	
	static class DisseminationDistance implements DisseminationDistanceInfo {
		
		private final int DDISTANCE = 5;

		@Override
		public int getMaxDistance() {
			return DDISTANCE;
		}
	}

	private final PeerID host = new PeerID("0");

	private final PeerID otherPeer = new PeerID("1");
	private final PeerID anotherPeer = new PeerID("2");
	private final PeerID anotherOnePeer = new PeerID("3");

	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();
	
	private static final DisseminationDistanceInfo disseminationInfo = new DisseminationDistance();

	@Test
	public void testAddLocalParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));

		UpdateTable updateTable = table.addLocalParameters(newParameters);
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-3", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		assertEquals(new EstimatedDistance(5, host), updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(5, host), updateTable.getAddition(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		updateTable = table.addLocalParameters(newParameters);

		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testParametersFromNeighbor() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(4, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(4, anotherPeer));

		Set<Parameter> parameters = table.getParameters(host);
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		parameters = table.getParameters(otherPeer);
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));

		parameters = table.getParameters(anotherPeer);
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
	}

	@Test
	public void testIsEmtpy() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);

		assertTrue(table.isEmpty());

		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));

		table.addLocalParameters(newParameters);

		assertFalse(table.isEmpty());
	}

	@Test
	public void testParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));

		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-3", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		final Set<Parameter> parameters = table.getParameters();

		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-3", emptyTaxonomy)));

		table.removeLocalParameters(newParameters);

		assertFalse(table.getParameters().isEmpty());
	}

	@Test
	public void testGetLocalParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));

		table.addLocalParameters(newParameters);

		final Set<Parameter> localParameters = table.getLocalParameters();

		assertTrue(localParameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(localParameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		table.removeLocalParameters(newParameters);

		assertTrue(table.getLocalParameters().isEmpty());
	}

	@Test
	public void testIsLocalParameter() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-3", emptyTaxonomy), new EstimatedDistance(2, host));

		assertTrue(table.isLocalParameter(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(table.isLocalParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		assertFalse(table.isLocalParameter(ParameterFactory.createParameter("I-3", emptyTaxonomy)));

		assertFalse(table.isLocalParameter(ParameterFactory.createParameter("I-4", emptyTaxonomy)));
	}

	@Test
	public void testSubsumesLocalParameter() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = createTaxonomy();

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-2", taxonomy));
		newParameters.add(ParameterFactory.createParameter("I-3", taxonomy));
		table.addLocalParameters(newParameters);

		assertEquals(1, table.subsumesLocalParameter(ParameterFactory.createParameter("I-1", taxonomy)).size());
		assertTrue(table.subsumesLocalParameter(ParameterFactory.createParameter("I-1", taxonomy)).contains(ParameterFactory.createParameter("I-2", taxonomy)));

		assertEquals(1, table.subsumesLocalParameter(ParameterFactory.createParameter("I-2", taxonomy)).size());
		assertTrue(table.subsumesLocalParameter(ParameterFactory.createParameter("I-2", taxonomy)).contains(ParameterFactory.createParameter("I-2", taxonomy)));
	}

	@Test
	public void testRemoveLocalParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));

		table.addLocalParameters(newParameters);

		final Set<Parameter> removedParameters = new HashSet<Parameter>();
		removedParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		removedParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));

		UpdateTable updateTable = table.removeLocalParameters(removedParameters);

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		updateTable = table.removeLocalParameters(removedParameters);

		assertTrue(updateTable.isEmpty());

		assertTrue(table.isEmpty());
	}

	@Test
	public void testGetNewNeighborTable() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		table.addEntry(ParameterFactory.createParameter("I-3", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		final UpdateTable newNeighbor = table.getNewNeighborTable();

		final Set<Parameter> parameters = newNeighbor.getParameters();

		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
		assertFalse(parameters.contains(ParameterFactory.createParameter("I-3", emptyTaxonomy)));
	}

	@Test
	public void testDecrementEstimatedDistances() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		table.decEstimatedDistances();

		for (final Parameter p : table.getParameters())
			assertEquals(disseminationInfo.getMaxDistance() - 1, table.getEstimatedDistance(p));
	}

	@Test
	public void testGetDistance() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(4, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(2, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-3", emptyTaxonomy), new EstimatedDistance(1, anotherPeer));

		assertEquals(4, table.getDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer));
		assertEquals(2, table.getDistance(ParameterFactory.createParameter("I-2", emptyTaxonomy), anotherPeer));
		assertEquals(1, table.getDistance(ParameterFactory.createParameter("I-3", emptyTaxonomy), anotherPeer));
		assertEquals(0, table.getDistance(ParameterFactory.createParameter("I-8", emptyTaxonomy), anotherPeer));
		assertEquals(0, table.getDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy), anotherOnePeer));
	}

	@Test
	public void testUpdateEmptyTable() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer"
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);

		// Get the table, from peer "host" which is sent to new neighbors and
		// update peer "otherPeer" with it
		final UpdateTable newNeighborTable = table.getNewNeighborTable();
		final UpdateTable updateTable = table2.updateTable(newNeighborTable, host).getUpdateTable();

		// Check that parameters where correctly added
		final Set<Parameter> parameters = table2.getParameters();
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that table is correctly updated
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check obtained update table is correct
		assertEquals(new EstimatedDistance(4, host), updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(4, host), updateTable.getAddition(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
	}

	@Test
	public void testUpdateEmptyTableNonGreaterThanZero() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(2, anotherPeer));

		// Create an empty table in the peer "otherPeer"
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);

		// Get the table, from peer "host" which is sent to new neighbors and
		// update peer "otherPeer" with it
		final UpdateTable newNeighborTable = table.getNewNeighborTable();
		final UpdateTable updateTable = table2.updateTable(newNeighborTable, host).getUpdateTable();

		// Check that parameters where correctly added
		final Set<Parameter> parameters = table2.getParameters();
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));

		// Check that update table does not contain parameter A because it is
		// not greater than 1 in current node
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));

		// Check that table2 contains the new parameter with a estimated
		// distance of 1
		assertEquals(new EstimatedDistance(1, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
	}

	@Test
	public void testSameUpdateHasNoEffect() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer"
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);

		// Get the table, from peer "host" which is sent to new neighbors and
		// update peer "otherPeer" with it
		UpdateTable newNeighborTable = table.getNewNeighborTable();
		UpdateTable updateTable = table2.updateTable(newNeighborTable, host).getUpdateTable();

		// Apply same update again
		newNeighborTable = table.getNewNeighborTable();
		updateTable = table2.updateTable(newNeighborTable, host).getUpdateTable();

		// Check that parameters where correctly added
		final Set<Parameter> parameters = table2.getParameters();
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that table is correctly updated
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that second obtained update table is empty
		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testAddAnotherTableFromOtherPeer() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer" and add first one
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);
		final UpdateTable newNeighborTable = table.getNewNeighborTable();
		table2.updateTable(newNeighborTable, host);

		// Add a table received from anotherPeer which contains values obtained
		// from a anotherOnePeer
		final UpdateTable anotherPeerTable = new UpdateTable();
		anotherPeerTable.setAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy), 6, anotherOnePeer);
		anotherPeerTable.setAddition(ParameterFactory.createParameter("I-2", emptyTaxonomy), 3, anotherOnePeer);

		// Check parameters before update
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// This update includes an update with a greater distance and an update
		// with a smaller one than the values hold currently by table2
		final UpdateTable updateTable = table2.updateTable(anotherPeerTable, anotherPeer).getUpdateTable();

		// Check that parameter A effective value has changed in table2
		assertEquals(new EstimatedDistance(5, anotherPeer), table2.getEffectiveDistance(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		// Check that parameter B effective value has not changed
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that the change for parameter A is contained in the update
		// table
		assertEquals(new EstimatedDistance(5, anotherPeer), updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		// Check that there is no change for parameter B and therefore is not
		// included in updateTable
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that parameter A contains two values in table2. One coming from
		// host and the other from anotherPeer
		assertEquals(2, table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-1", emptyTaxonomy)).getList().size());
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-1", emptyTaxonomy)).getList().contains(new EstimatedDistance(4, host)));
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-1", emptyTaxonomy)).getList().contains(new EstimatedDistance(5, anotherPeer)));

		// Check that parameter B contains two values in table2. One coming from
		// host and the other from anotherPeer
		assertEquals(2, table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-2", emptyTaxonomy)).getList().size());
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-2", emptyTaxonomy)).getList().contains(new EstimatedDistance(4, host)));
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-2", emptyTaxonomy)).getList().contains(new EstimatedDistance(2, anotherPeer)));
	}

	@Test
	public void testRemovalElementNotGreaterThanOne() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(1, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-2", emptyTaxonomy), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check that parameters have been removed
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));

		// Check that update table is empty
		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testRemovalNonExistentElement() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-2", emptyTaxonomy), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check that table is empty
		assertTrue(table.isEmpty());

		// Check that update table is empty
		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testRemovalElementGreaterThanOneNoNewEffectiveDistance() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-2", emptyTaxonomy), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check parameters
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that update table contains a removal for A but not for B
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
	}

	@Test
	public void testRemovalElementGreaterThanOneWithEffectiveDistanceGreaterThanOne() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(2, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-2", emptyTaxonomy), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check parameters
		assertTrue(table.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that update table contains a removal for A but not for B
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertEquals(new EstimatedDistance(2, anotherPeer), updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
	}

	@Test
	public void testRemovalElementGreaterThanOneWithEffectiveDistanceNotGreaterThanOne() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(1, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-2", emptyTaxonomy), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check parameters
		assertTrue(table.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		// Check that update table contains a removal for A but not for B
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
	}

	@Test
	public void testRemovalInsertionListNotChanged() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(4, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(2, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);
		removalTable.setAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy), 4, otherPeer);
		removalTable.setAddition(ParameterFactory.createParameter("I-2", emptyTaxonomy), 4, otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// The update table does not contain updates for parameter A but for B.
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
	}

	@Test
	public void testUpdateWithDataComingfromSameNode() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer" and add first one
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);
		UpdateTable newNeighborTable = table.getNewNeighborTable();
		table2.updateTable(newNeighborTable, host);

		// Add a parameter obtained from anotherPeer
		table2.addEntry(ParameterFactory.createParameter("I-3", emptyTaxonomy), new EstimatedDistance(4, anotherPeer));
		table2.addEntry(ParameterFactory.createParameter("I-4", emptyTaxonomy), new EstimatedDistance(1, anotherPeer));

		// Obtain neighbor table from other peer and send it again to host
		newNeighborTable = table2.getNewNeighborTable();

		// Add table to peer host
		final UpdateTable updateTable = table.updateTable(newNeighborTable, otherPeer).getUpdateTable();

		// Table from host node contains a new entry for parameter C
		assertTrue(table.getParameters().contains(ParameterFactory.createParameter("I-3", emptyTaxonomy)));
		// No entry for parameter D because it was not sent in newNeighborTable
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-4", emptyTaxonomy)));

		// Table from host node has not changed for parameters A and B
		assertEquals(1, table.getEffectiveDistanceList(ParameterFactory.createParameter("I-1", emptyTaxonomy)).getList().size());
		assertEquals(1, table.getEffectiveDistanceList(ParameterFactory.createParameter("I-2", emptyTaxonomy)).getList().size());

		// And update table only contains an entry for parameter C
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-3", emptyTaxonomy)));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-4", emptyTaxonomy)));
	}

	@Test
	public void testXMLSerialization() throws IOException, InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		table.addLocalParameters(newParameters);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		table.saveToXML(baos);
		baos.close();

		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		final ParameterTable otherTable = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		otherTable.readFromXML(bais);

		assertEquals(table, otherTable);
	}

	// Detected problems
	@Test
	public void testRemoveNonExistentParameter() throws InvalidParameterIDException {
		// Create empty table
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);

		// Remove inexistent parameter
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testUpdateIssue() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, new PeerID("2"), emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(3, new PeerID("1")));
		table.addEntry(ParameterFactory.createParameter("I-1", emptyTaxonomy), new EstimatedDistance(4, new PeerID("0")));
		table.addEntry(ParameterFactory.createParameter("I-2", emptyTaxonomy), new EstimatedDistance(2, new PeerID("3")));
		table.addEntry(ParameterFactory.createParameter("O-3", emptyTaxonomy), new EstimatedDistance(2, new PeerID("3")));
		table.addEntry(ParameterFactory.createParameter("O-2", emptyTaxonomy), new EstimatedDistance(3, new PeerID("1")));
		table.addEntry(ParameterFactory.createParameter("O-2", emptyTaxonomy), new EstimatedDistance(4, new PeerID("0")));

		final UpdateTable neighborUpdateTable = new UpdateTable();
		neighborUpdateTable.setDelete(ParameterFactory.createParameter("I-1", emptyTaxonomy), new PeerID("1"));
		neighborUpdateTable.setDelete(ParameterFactory.createParameter("O-2", emptyTaxonomy), new PeerID("1"));
		neighborUpdateTable.setAddition(ParameterFactory.createParameter("I-1", emptyTaxonomy), 3, new PeerID("2"));
		neighborUpdateTable.setAddition(ParameterFactory.createParameter("O-2", emptyTaxonomy), 2, new PeerID("2"));

		final UpdateTable updateTable = table.updateTable(neighborUpdateTable, new PeerID("1")).getUpdateTable();

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", emptyTaxonomy)));

		assertEquals(new EstimatedDistance(0, new PeerID("2")), updateTable.getDeletion(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
	}

	@Test
	public void testTaxonomyUpdate() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = createTaxonomy();

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);

		table.addEntry(ParameterFactory.createParameter("I-2", taxonomy), new EstimatedDistance(2, otherPeer));

		final UpdateTable neighborUpdateTable = new UpdateTable();
		neighborUpdateTable.setAddition(ParameterFactory.createParameter("I-1", taxonomy), 4, anotherPeer);

		final UpdateTable updateTable = table.updateTable(neighborUpdateTable, anotherOnePeer).getUpdateTable();

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", taxonomy)));

		assertEquals(3, table.getEstimatedDistance(ParameterFactory.createParameter("I-1", taxonomy)));
		assertEquals(3, table.getEstimatedDistance(ParameterFactory.createParameter("I-2", taxonomy)));
	}

	@Test
	public void testAddLocalParameterMoreGeneral() throws TaxonomyException, InvalidParameterIDException {
		final Taxonomy taxonomy = createTaxonomy();

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);
		Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-2", taxonomy));
		UpdateTable updateTable = table.addLocalParameters(parameters);

		assertEquals(1, updateTable.getParameters().size());
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-2", taxonomy)));

		parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-1", taxonomy));
		updateTable = table.addLocalParameters(parameters);

		assertEquals(1, updateTable.getParameters().size());
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", taxonomy)));
	}

	private Taxonomy createTaxonomy() throws TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "1");
		taxonomy.addChild("Z", "3");
		taxonomy.addChild("1", "2");
		return taxonomy;
	}

	@Test
	public void testAddLocalParameterMoreSpecific() throws TaxonomyException, InvalidParameterIDException {
		final Taxonomy taxonomy = createTaxonomy();

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);
		Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-1", taxonomy));
		UpdateTable updateTable = table.addLocalParameters(parameters);

		assertEquals(1, updateTable.getParameters().size());
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-1", taxonomy)));

		parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-2", taxonomy));
		updateTable = table.addLocalParameters(parameters);

		assertTrue(updateTable.isEmpty());
	}
}
