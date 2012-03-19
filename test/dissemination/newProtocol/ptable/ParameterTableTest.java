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
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));

		UpdateTable updateTable = table.addLocalParameters(newParameters);
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-C"), new EstimatedDistance(1, otherPeer));

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));

		assertEquals(new EstimatedDistance(5, host), updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(5, host), updateTable.getAddition(ParameterFactory.createParameter("I-B")));

		updateTable = table.addLocalParameters(newParameters);

		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testParametersFromNeighbor() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(4, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(4, anotherPeer));

		Set<Parameter> parameters = table.getParameters(host);
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-B")));

		parameters = table.getParameters(otherPeer);
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));

		parameters = table.getParameters(anotherPeer);
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testIsEmtpy() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);

		assertTrue(table.isEmpty());

		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));

		table.addLocalParameters(newParameters);

		assertFalse(table.isEmpty());
	}

	@Test
	public void testParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));

		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-C"), new EstimatedDistance(1, otherPeer));

		final Set<Parameter> parameters = table.getParameters();

		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-B")));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-C")));

		table.removeLocalParameters(newParameters);

		assertFalse(table.getParameters().isEmpty());
	}

	@Test
	public void testGetLocalParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));

		table.addLocalParameters(newParameters);

		final Set<Parameter> localParameters = table.getLocalParameters();

		assertTrue(localParameters.contains(ParameterFactory.createParameter("I-A")));
		assertTrue(localParameters.contains(ParameterFactory.createParameter("I-B")));

		table.removeLocalParameters(newParameters);

		assertTrue(table.getLocalParameters().isEmpty());
	}

	@Test
	public void testIsLocalParameter() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-C"), new EstimatedDistance(2, host));

		assertTrue(table.isLocalParameter(ParameterFactory.createParameter("I-A")));
		assertTrue(table.isLocalParameter(ParameterFactory.createParameter("I-B")));

		assertFalse(table.isLocalParameter(ParameterFactory.createParameter("I-C")));

		assertFalse(table.isLocalParameter(ParameterFactory.createParameter("I-D")));
	}

	@Test
	public void testSubsumesLocalParameter() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-B"));
		newParameters.add(ParameterFactory.createParameter("I-C"));
		table.addLocalParameters(newParameters);

		assertEquals(1, table.subsumesLocalParameter(ParameterFactory.createParameter("I-A")).size());
		assertTrue(table.subsumesLocalParameter(ParameterFactory.createParameter("I-A")).contains(ParameterFactory.createParameter("I-B")));

		assertEquals(1, table.subsumesLocalParameter(ParameterFactory.createParameter("I-B")).size());
		assertTrue(table.subsumesLocalParameter(ParameterFactory.createParameter("I-B")).contains(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testRemoveLocalParameters() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));

		table.addLocalParameters(newParameters);

		final Set<Parameter> removedParameters = new HashSet<Parameter>();
		removedParameters.add(ParameterFactory.createParameter("I-A"));
		removedParameters.add(ParameterFactory.createParameter("I-B"));

		UpdateTable updateTable = table.removeLocalParameters(removedParameters);

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));

		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-B")));

		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-B")));

		updateTable = table.removeLocalParameters(removedParameters);

		assertTrue(updateTable.isEmpty());

		assertTrue(table.isEmpty());
	}

	@Test
	public void testGetNewNeighborTable() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);

		table.addEntry(ParameterFactory.createParameter("I-C"), new EstimatedDistance(1, otherPeer));

		final UpdateTable newNeighbor = table.getNewNeighborTable();

		final Set<Parameter> parameters = newNeighbor.getParameters();

		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-B")));
		assertFalse(parameters.contains(ParameterFactory.createParameter("I-C")));
	}

	@Test
	public void testDecrementEstimatedDistances() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);

		table.decEstimatedDistances();

		for (final Parameter p : table.getParameters())
			assertEquals(disseminationInfo.getMaxDistance() - 1, table.getEstimatedDistance(p));
	}

	@Test
	public void testGetDistance() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(4, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(2, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-C"), new EstimatedDistance(1, anotherPeer));

		assertEquals(4, table.getDistance(ParameterFactory.createParameter("I-A"), otherPeer));
		assertEquals(2, table.getDistance(ParameterFactory.createParameter("I-B"), anotherPeer));
		assertEquals(1, table.getDistance(ParameterFactory.createParameter("I-C"), anotherPeer));
		assertEquals(0, table.getDistance(ParameterFactory.createParameter("I-H"), anotherPeer));
		assertEquals(0, table.getDistance(ParameterFactory.createParameter("I-A"), anotherOnePeer));
	}

	@Test
	public void testUpdateEmptyTable() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer"
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);

		// Get the table, from peer "host" which is sent to new neighbors and
		// update peer "otherPeer" with it
		final UpdateTable newNeighborTable = table.getNewNeighborTable();
		final UpdateTable updateTable = table2.updateTable(newNeighborTable, host).getUpdateTable();

		// Check that parameters where correctly added
		final Set<Parameter> parameters = table2.getParameters();
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-B")));

		// Check that table is correctly updated
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-B")));

		// Check obtained update table is correct
		assertEquals(new EstimatedDistance(4, host), updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(4, host), updateTable.getAddition(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testUpdateEmptyTableNonGreaterThanZero() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(2, anotherPeer));

		// Create an empty table in the peer "otherPeer"
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);

		// Get the table, from peer "host" which is sent to new neighbors and
		// update peer "otherPeer" with it
		final UpdateTable newNeighborTable = table.getNewNeighborTable();
		final UpdateTable updateTable = table2.updateTable(newNeighborTable, host).getUpdateTable();

		// Check that parameters where correctly added
		final Set<Parameter> parameters = table2.getParameters();
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));

		// Check that update table does not contain parameter A because it is
		// not greater than 1 in current node
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));

		// Check that table2 contains the new parameter with a estimated
		// distance of 1
		assertEquals(new EstimatedDistance(1, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-A")));
	}

	@Test
	public void testSameUpdateHasNoEffect() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
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
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-A")));
		assertTrue(parameters.contains(ParameterFactory.createParameter("I-B")));

		// Check that table is correctly updated
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-B")));

		// Check that second obtained update table is empty
		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testAddAnotherTableFromOtherPeer() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer" and add first one
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);
		final UpdateTable newNeighborTable = table.getNewNeighborTable();
		table2.updateTable(newNeighborTable, host);

		// Add a table received from anotherPeer which contains values obtained
		// from a anotherOnePeer
		final UpdateTable anotherPeerTable = new UpdateTable();
		anotherPeerTable.setAddition(ParameterFactory.createParameter("I-A"), 6, anotherOnePeer);
		anotherPeerTable.setAddition(ParameterFactory.createParameter("I-B"), 3, anotherOnePeer);

		// Check parameters before update
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-B")));

		// This update includes an update with a greater distance and an update
		// with a smaller one than the values hold currently by table2
		final UpdateTable updateTable = table2.updateTable(anotherPeerTable, anotherPeer).getUpdateTable();

		// Check that parameter A effective value has changed in table2
		assertEquals(new EstimatedDistance(5, anotherPeer), table2.getEffectiveDistance(ParameterFactory.createParameter("I-A")));
		// Check that parameter B effective value has not changed
		assertEquals(new EstimatedDistance(4, host), table2.getEffectiveDistance(ParameterFactory.createParameter("I-B")));

		// Check that the change for parameter A is contained in the update
		// table
		assertEquals(new EstimatedDistance(5, anotherPeer), updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		// Check that there is no change for parameter B and therefore is not
		// included in updateTable
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));

		// Check that parameter A contains two values in table2. One coming from
		// host and the other from anotherPeer
		assertEquals(2, table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-A")).getList().size());
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-A")).getList().contains(new EstimatedDistance(4, host)));
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-A")).getList().contains(new EstimatedDistance(5, anotherPeer)));

		// Check that parameter B contains two values in table2. One coming from
		// host and the other from anotherPeer
		assertEquals(2, table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-B")).getList().size());
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-B")).getList().contains(new EstimatedDistance(4, host)));
		assertTrue(table2.getEffectiveDistanceList(ParameterFactory.createParameter("I-B")).getList().contains(new EstimatedDistance(2, anotherPeer)));
	}

	@Test
	public void testRemovalElementNotGreaterThanOne() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(1, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-B"), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check that parameters have been removed
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-A")));

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
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-B"), otherPeer);

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
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-B"), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check parameters
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-B")));

		// Check that update table contains a removal for A but not for B
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-A")));
		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testRemovalElementGreaterThanOneWithEffectiveDistanceGreaterThanOne() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(2, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-B"), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check parameters
		assertTrue(table.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-B")));

		// Check that update table contains a removal for A but not for B
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-A")));
		assertEquals(new EstimatedDistance(2, anotherPeer), updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testRemovalElementGreaterThanOneWithEffectiveDistanceNotGreaterThanOne() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(1, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(1, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);
		removalTable.setDelete(ParameterFactory.createParameter("I-B"), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// Check parameters
		assertTrue(table.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-B")));

		// Check that update table contains a removal for A but not for B
		assertEquals(new EstimatedDistance(0, host), updateTable.getDeletion(ParameterFactory.createParameter("I-A")));
		assertNull(updateTable.getAddition(ParameterFactory.createParameter("I-A")));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testRemovalInsertionListNotChanged() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(3, otherPeer));
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(4, anotherPeer));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(2, otherPeer));

		// Create a table to remove parameter A and B received from node
		// otherPeer
		final UpdateTable removalTable = new UpdateTable();
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);
		removalTable.setAddition(ParameterFactory.createParameter("I-A"), 4, otherPeer);
		removalTable.setAddition(ParameterFactory.createParameter("I-B"), 4, otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		// The update table does not contain updates for parameter A but for B.
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testUpdateWithDataComingfromSameNode() throws InvalidParameterIDException {
		// Create a table in peer "host" with two local parameters
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
		table.addLocalParameters(newParameters);

		// Create an empty table in the peer "otherPeer" and add first one
		final ParameterTable table2 = new ParameterTable(disseminationInfo, otherPeer, emptyTaxonomy);
		UpdateTable newNeighborTable = table.getNewNeighborTable();
		table2.updateTable(newNeighborTable, host);

		// Add a parameter obtained from anotherPeer
		table2.addEntry(ParameterFactory.createParameter("I-C"), new EstimatedDistance(4, anotherPeer));
		table2.addEntry(ParameterFactory.createParameter("I-D"), new EstimatedDistance(1, anotherPeer));

		// Obtain neighbor table from other peer and send it again to host
		newNeighborTable = table2.getNewNeighborTable();

		// Add table to peer host
		final UpdateTable updateTable = table.updateTable(newNeighborTable, otherPeer).getUpdateTable();

		// Table from host node contains a new entry for parameter C
		assertTrue(table.getParameters().contains(ParameterFactory.createParameter("I-C")));
		// No entry for parameter D because it was not sent in newNeighborTable
		assertFalse(table.getParameters().contains(ParameterFactory.createParameter("I-D")));

		// Table from host node has not changed for parameters A and B
		assertEquals(1, table.getEffectiveDistanceList(ParameterFactory.createParameter("I-A")).getList().size());
		assertEquals(1, table.getEffectiveDistanceList(ParameterFactory.createParameter("I-B")).getList().size());

		// And update table only contains an entry for parameter C
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-C")));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));
		assertFalse(updateTable.getParameters().contains(ParameterFactory.createParameter("I-D")));
	}

	@Test
	public void testXMLSerialization() throws IOException, InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, host, emptyTaxonomy);
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("I-A"));
		newParameters.add(ParameterFactory.createParameter("I-B"));
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
		removalTable.setDelete(ParameterFactory.createParameter("I-A"), otherPeer);

		final UpdateTable updateTable = table.updateTable(removalTable, otherPeer).getUpdateTable();

		assertTrue(updateTable.isEmpty());
	}

	@Test
	public void testUpdateIssue() throws InvalidParameterIDException {
		final ParameterTable table = new ParameterTable(disseminationInfo, new PeerID("2"), emptyTaxonomy);
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(3, new PeerID("1")));
		table.addEntry(ParameterFactory.createParameter("I-A"), new EstimatedDistance(4, new PeerID("0")));
		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(2, new PeerID("3")));
		table.addEntry(ParameterFactory.createParameter("O-C"), new EstimatedDistance(2, new PeerID("3")));
		table.addEntry(ParameterFactory.createParameter("O-B"), new EstimatedDistance(3, new PeerID("1")));
		table.addEntry(ParameterFactory.createParameter("O-B"), new EstimatedDistance(4, new PeerID("0")));

		final UpdateTable neighborUpdateTable = new UpdateTable();
		neighborUpdateTable.setDelete(ParameterFactory.createParameter("I-A"), new PeerID("1"));
		neighborUpdateTable.setDelete(ParameterFactory.createParameter("O-B"), new PeerID("1"));
		neighborUpdateTable.setAddition(ParameterFactory.createParameter("I-A"), 3, new PeerID("2"));
		neighborUpdateTable.setAddition(ParameterFactory.createParameter("O-B"), 2, new PeerID("2"));

		final UpdateTable updateTable = table.updateTable(neighborUpdateTable, new PeerID("1")).getUpdateTable();

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));

		assertEquals(new EstimatedDistance(0, new PeerID("2")), updateTable.getDeletion(ParameterFactory.createParameter("I-A")));
	}

	@Test
	public void testTaxonomyUpdate() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);

		table.addEntry(ParameterFactory.createParameter("I-B"), new EstimatedDistance(2, otherPeer));

		final UpdateTable neighborUpdateTable = new UpdateTable();
		neighborUpdateTable.setAddition(ParameterFactory.createParameter("I-A"), 4, anotherPeer);

		final UpdateTable updateTable = table.updateTable(neighborUpdateTable, anotherOnePeer).getUpdateTable();

		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));

		assertEquals(3, table.getEstimatedDistance(ParameterFactory.createParameter("I-A")));
		assertEquals(3, table.getEstimatedDistance(ParameterFactory.createParameter("I-B")));
	}

	@Test
	public void testAddLocalParameterMoreGeneral() throws TaxonomyException, InvalidParameterIDException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);
		Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-B"));
		UpdateTable updateTable = table.addLocalParameters(parameters);

		assertEquals(1, updateTable.getParameters().size());
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-B")));

		parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-A"));
		updateTable = table.addLocalParameters(parameters);

		assertEquals(1, updateTable.getParameters().size());
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));
	}

	@Test
	public void testAddLocalParameterMoreSpecific() throws TaxonomyException, InvalidParameterIDException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final ParameterTable table = new ParameterTable(disseminationInfo, host, taxonomy);
		Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-A"));
		UpdateTable updateTable = table.addLocalParameters(parameters);

		assertEquals(1, updateTable.getParameters().size());
		assertTrue(updateTable.getParameters().contains(ParameterFactory.createParameter("I-A")));

		parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-B"));
		updateTable = table.addLocalParameters(parameters);

		assertTrue(updateTable.isEmpty());
	}
}
