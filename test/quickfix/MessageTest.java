package quickfix;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;
import quickfix.field.AllocAccount;
import quickfix.field.AllocShares;
import quickfix.field.BeginString;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.ClOrdID;
import quickfix.field.CountryOfIssue;
import quickfix.field.CrossID;
import quickfix.field.CrossPrioritization;
import quickfix.field.CrossType;
import quickfix.field.EncryptMethod;
import quickfix.field.IOIid;
import quickfix.field.MsgDirection;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.PartyID;
import quickfix.field.PartyIDSource;
import quickfix.field.PartyRole;
import quickfix.field.Price;
import quickfix.field.RefMsgType;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.SecurityType;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.field.TransactTime;
import quickfix.field.UnderlyingCurrency;
import quickfix.field.UnderlyingSymbol;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.IndicationOfInterest;
import quickfix.fix44.Logon;
import quickfix.fix44.NewOrderCross;
import quickfix.fix44.Logon.NoMsgTypes;
import quickfix.fix44.component.Instrument;

public class MessageTest extends TestCase {

    public void testParsing() throws Exception {
        // checksum is not verified in these tests
        Message message = new Message("8=FIX.4.2\0019=40\00135=A\001"
                + "98=0\001384=2\001372=D\001385=R\001372=8\001385=S\00110=96\001",
                DataDictionaryTest.getDictionary());

        assertHeaderField(message, "FIX.4.2", BeginString.FIELD);
        assertHeaderField(message, "40", BodyLength.FIELD);
        assertEquals("wrong field value", 40, message.getHeader().getInt(BodyLength.FIELD));
        assertHeaderField(message, "A", MsgType.FIELD);
        assertBodyField(message, "0", EncryptMethod.FIELD);
        assertTrailerField(message, "96", CheckSum.FIELD);
        NoMsgTypes valueMessageType = new Logon.NoMsgTypes();
        message.getGroup(1, valueMessageType);
        assertEquals("wrong value", "D", valueMessageType.getString(RefMsgType.FIELD));
        assertEquals("wrong value", "R", valueMessageType.getString(MsgDirection.FIELD));
        message.getGroup(2, valueMessageType);
        assertEquals("wrong value", "8", valueMessageType.getString(RefMsgType.FIELD));
        assertEquals("wrong value", "S", valueMessageType.getString(MsgDirection.FIELD));
    }

    public void testValidation() throws Exception {
        String data = "8=FIX.4.49=30935=849=ASX56=CL1_FIX4434=452=20060324-01:05:58"
                + "17=X-B-WOW-1494E9A0:58BD3F9D-1109150=D39=011=18427138=200198=1494E9A0:58BD3F9D"
                + "526=432437=B-WOW-1494E9A0:58BD3F9D55=WOW54=1151=20014=040=244=1559=16=0"
                + "453=3448=AAA35791447=D452=3448=8447=D452=4448=FIX11447=D452=36"
                + "60=20060320-03:34:2910=169";
        ExecutionReport executionReport = new ExecutionReport();
        DataDictionary dictionary = DataDictionaryTest.getDictionary();
        assertNotNull(dictionary);
        executionReport.fromString(data, dictionary, true);
        dictionary.validate(executionReport);
    }

    public void testCalculateStringWithNestedGroups() throws Exception {
        NewOrderCross noc = new NewOrderCross();
        noc.getHeader().setString(BeginString.FIELD, FixVersions.BEGINSTRING_FIX44);
        noc.getHeader().setInt(MsgSeqNum.FIELD, 5);
        noc.getHeader().setString(SenderCompID.FIELD, "sender");
        noc.getHeader().setString(TargetCompID.FIELD, "target");
        noc.getHeader().setString(SendingTime.FIELD, "20060319-09:08:20.881");

        noc.setString(SecurityIDSource.FIELD, SecurityIDSource.EXCHANGE_SYMBOL);
        noc.setChar(OrdType.FIELD, OrdType.LIMIT);
        noc.setDouble(Price.FIELD, 9.00);
        noc.setString(SecurityID.FIELD, "ABC");
        noc.setString(Symbol.FIELD, "ABC");
        noc.setString(TransactTime.FIELD, "20060319-09:08:19");
        noc.setString(CrossID.FIELD, "184214");
        noc.setInt(CrossType.FIELD,
                CrossType.CROSS_TRADE_WHICH_IS_EXECUTED_PARTIALLY_AND_THE_REST_IS_CANCELLED);
        noc.setInt(CrossPrioritization.FIELD, CrossPrioritization.NONE);

        NewOrderCross.NoSides side = new NewOrderCross.NoSides();
        side.setChar(Side.FIELD, Side.BUY);
        side.setDouble(OrderQty.FIELD, 9);

        NewOrderCross.NoSides.NoPartyIDs party = new NewOrderCross.NoSides.NoPartyIDs();
        party.setString(PartyID.FIELD, "8");
        party.setChar(PartyIDSource.FIELD, PartyIDSource.PROPRIETARY_CUSTOM_CODE);
        party.setInt(PartyRole.FIELD, PartyRole.CLEARING_FIRM);

        side.addGroup(party);

        party.setString(PartyID.FIELD, "AAA35777");
        party.setChar(PartyIDSource.FIELD, PartyIDSource.PROPRIETARY_CUSTOM_CODE);
        party.setInt(PartyRole.FIELD, PartyRole.CLIENT_ID);

        side.addGroup(party);

        noc.addGroup(side);

        side.clear();
        side.setChar(Side.FIELD, Side.SELL);
        side.setDouble(OrderQty.FIELD, 9);

        party.clear();
        party.setString(PartyID.FIELD, "8");
        party.setChar(PartyIDSource.FIELD, PartyIDSource.PROPRIETARY_CUSTOM_CODE);
        party.setInt(PartyRole.FIELD, PartyRole.CLEARING_FIRM);
        side.addGroup(party);

        party.clear();
        party.setString(PartyID.FIELD, "aaa");
        party.setChar(PartyIDSource.FIELD, PartyIDSource.PROPRIETARY_CUSTOM_CODE);
        party.setInt(PartyRole.FIELD, PartyRole.CLIENT_ID);
        side.addGroup(party);

        noc.addGroup(side);

        String expectedMessage = "8=FIX.4.49=24735=s34=549=sender52=20060319-09:08:20.881"
                + "56=target22=840=244=948=ABC55=ABC60=20060319-09:08:19548=184214549=2"
                + "550=0552=254=1453=2448=8447=D452=4448=AAA35777447=D452=338=954=2"
                + "453=2448=8447=D452=4448=aaa447=D452=338=910=056";
        assertEquals("wrong message", expectedMessage, noc.toString());

    }

    public void testFieldOrdering() throws Exception {
        String expectedMessageString = "8=FIX.4.49=17135=D49=SenderCompId56=TargetCompId11=183339"
                + "22=838=140=244=1248=BHP54=255=BHP59=160=20060223-22:38:33526=3620453=2448=8"
                + "447=D452=4448=AAA35354447=D452=310=168";
        DataDictionary dataDictionary = new DataDictionary("etc/FIX44.xml");
        Message message = new DefaultMessageFactory().create(dataDictionary.getVersion(), "D");
        message.fromString(expectedMessageString, dataDictionary, false);
        String actualMessageString = message.toString();
        System.out.println(actualMessageString);
        System.out.println(actualMessageString
                .indexOf("453=2448=8447=D452=4448=AAA35354447=D452=3"));
        assertTrue("wrong field ordering", actualMessageString
                .indexOf("453=2448=8447=D452=4448=AAA35354447=D452=3") != -1);
    }

    public void testParsing2() throws Exception {
        // checksum is not verified in these tests
        String data = "8=FIX.4.2\0019=76\001";
        data += "35=6\001";
        data += "23=IDENTIFIER\001";
        data += "28=N\001";
        data += "55=MSFT\001";
        data += "54=1\001";
        data += "711=2\001";
        data += "311=DELL\001";
        data += "318=USD\001";
        data += "311=IBM\001";
        data += "318=CAD\001";
        data += "10=037\001";
        Message message = new Message(data, DataDictionaryTest.getDictionary());

        assertHeaderField(message, "FIX.4.2", BeginString.FIELD);
        assertHeaderField(message, "76", BodyLength.FIELD);
        assertHeaderField(message, MsgType.INDICATION_OF_INTEREST, MsgType.FIELD);
        assertBodyField(message, "IDENTIFIER", IOIid.FIELD);
        assertTrailerField(message, "037", CheckSum.FIELD);
        IndicationOfInterest.NoUnderlyings valueMessageType = new IndicationOfInterest.NoUnderlyings();
        message.getGroup(1, valueMessageType);
        assertEquals("wrong value", "DELL", valueMessageType.getString(UnderlyingSymbol.FIELD));
        assertEquals("wrong value", "USD", valueMessageType.getString(UnderlyingCurrency.FIELD));
        message.getGroup(2, valueMessageType);
        assertEquals("wrong value", "IBM", valueMessageType.getString(UnderlyingSymbol.FIELD));
        assertEquals("wrong value", "CAD", valueMessageType.getString(UnderlyingCurrency.FIELD));
    }

    private void assertHeaderField(Message message, String expectedValue, int field)
            throws FieldNotFound {
        assertEquals(expectedValue, message.getHeader().getString(field));
        assertFieldNotFound(message, field);
        assertFieldNotFound(message.getTrailer(), field);
    }

    private void assertFieldNotFound(FieldMap message, int field) {
        try {
            message.getString(field);
            fail("field shouldn't be here: " + field);
        } catch (FieldNotFound e) {
            // expected
        }
    }

    private void assertBodyField(Message message, String expectedValue, int field)
            throws FieldNotFound {
        assertEquals(expectedValue, message.getString(field));
        assertFieldNotFound(message.getHeader(), field);
        assertFieldNotFound(message.getTrailer(), field);
    }

    private void assertTrailerField(Message message, String expectedValue, int field)
            throws FieldNotFound {
        assertEquals(expectedValue, message.getTrailer().getString(field));
        assertFieldNotFound(message, field);
        assertFieldNotFound(message.getHeader(), field);
    }

    public void testHeaderOutOfOrder() throws Exception {
        try {
            Message message = new Message("1=FIX.4.2");
            assertFalse(message.hasValidStructure());
        } catch (InvalidMessage e) {
            // expected
        }
    }

    public void testMessageFromString() {
        Message message = null;

        boolean badMessage = false;
        try {
            message = new Message("8=FIX.4.2\0019=12\00135=A\001108=30\00110=036\001");
        } catch (InvalidMessage e) {
            badMessage = true;
        }
        assertTrue("Message should be invalid", badMessage);

        try {
            message = new Message("8=FIX.4.2\0019=12\00135=A\001108=30\00110=026\001");
        } catch (InvalidMessage e) {
            assertTrue("Message should be valid (" + e.getMessage() + ")", false);
        }
        assertEquals("8=FIX.4.2\0019=12\00135=A\001108=30\00110=026\001", message.toString());
    }

    public void testMessageGroups() {
        Message message = new Message();
        NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);

        assertGroupContent(message, numAllocs);
    }

    private void assertGroupContent(Message message, NewOrderSingle.NoAllocs numAllocs) {
        StringField field = null;
        java.util.Iterator i = numAllocs.iterator();
        assertTrue(i.hasNext());
        field = (StringField) i.next();
        assertEquals("AllocACC2", field.getValue());
        assertTrue(i.hasNext());
        field = (StringField) i.next();
        assertEquals("2020.2", field.getValue());
        assertTrue(!i.hasNext());

        try {
            message.getGroup(1, numAllocs);
            assertEquals("AllocACC1", numAllocs.getField(new AllocAccount()).getValue());
            assertTrue(1010.10 == numAllocs.getField(new AllocShares()).getValue());
            message.getGroup(2, numAllocs);
            assertEquals("AllocACC2", numAllocs.getField(new AllocAccount()).getValue());
            assertTrue(2020.20 == numAllocs.getField(new AllocShares()).getValue());
        } catch (FieldNotFound e) {
            fail("no exception should be thrown");
        }

        try {
            message.getGroup(3, numAllocs);
            fail("exception should be thrown");
        } catch (FieldNotFound e) {
        }
    }

    public void testMessageCloneWithGroups() {
        Message message = new Message();
        NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);

        Message clonedMessage = (Message) message.clone();
        assertGroupContent(clonedMessage, numAllocs);
    }

    public void testFieldOrderAfterClone() {
        Message message = new quickfix.fix44.NewOrderSingle();
        quickfix.fix44.NewOrderSingle.NoPartyIDs partyIdGroup = new quickfix.fix44.NewOrderSingle.NoPartyIDs();
        partyIdGroup.set(new PartyID("PARTY_1"));
        partyIdGroup.set(new PartyIDSource(PartyIDSource.DIRECTED_BROKER));
        partyIdGroup.set(new PartyRole(PartyRole.INTRODUCING_FIRM));
        message.addGroup(partyIdGroup);
        Message clonedMessage = (Message) message.clone();
        assertEquals("wrong field order",
                "8=FIX.4.49=3535=D453=1448=PARTY_1447=I452=610=040", clonedMessage
                        .toString());
    }

    public void testMessageGroupRemoval() {
        Message message = new Message();
        NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);

        assertEquals("wrong # of group members", 2, message.getGroupCount(numAllocs.getFieldTag()));

        message.removeGroup(numAllocs);

        assertEquals("wrong # of group members", 0, message.getGroupCount(numAllocs.getFieldTag()));

        numAllocs = setUpGroups(message);
        assertEquals("wrong # of group members", 2, message.getGroupCount(numAllocs.getFieldTag()));

        message.removeGroup(numAllocs.getFieldTag());

        assertEquals("wrong # of group members", 0, message.getGroupCount(numAllocs.getFieldTag()));

        numAllocs = setUpGroups(message);
        assertEquals("wrong # of group members", 2, message.getGroupCount(numAllocs.getFieldTag()));

        message.removeGroup(2, numAllocs.getFieldTag());

        assertEquals("wrong # of group members", 1, message.getGroupCount(numAllocs.getFieldTag()));

        message.removeGroup(1, numAllocs.getFieldTag());

        assertEquals("wrong # of group members", 0, message.getGroupCount(numAllocs.getFieldTag()));

        numAllocs = setUpGroups(message);
        assertEquals("wrong # of group members", 2, message.getGroupCount(numAllocs.getFieldTag()));

        message.removeGroup(2, numAllocs);

        assertEquals("wrong # of group members", 1, message.getGroupCount(numAllocs.getFieldTag()));

        message.removeGroup(1, numAllocs);

        assertEquals("wrong # of group members", 0, message.getGroupCount(numAllocs.getFieldTag()));

        // ensure no exception when groups are empty
        message.removeGroup(1, numAllocs);

        assertEquals("wrong # of group members", 0, message.getGroupCount(numAllocs.getFieldTag()));
    }

    public void testHasGroup() {
        Message message = new Message();
        NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);

        assertFalse("wrong value", message.hasGroup(654));
        assertTrue("wrong value", message.hasGroup(numAllocs.getFieldTag()));
        assertTrue("wrong value", message.hasGroup(numAllocs));
        assertTrue("wrong value", message.hasGroup(1, numAllocs));
        assertTrue("wrong value", message.hasGroup(1, numAllocs.getFieldTag()));
        assertTrue("wrong value", message.hasGroup(2, numAllocs));
        assertTrue("wrong value", message.hasGroup(2, numAllocs.getFieldTag()));
        assertFalse("wrong value", message.hasGroup(3, numAllocs));
        assertFalse("wrong value", message.hasGroup(3, numAllocs.getFieldTag()));
    }

    private NewOrderSingle.NoAllocs setUpGroups(Message message) {
        NewOrderSingle.NoAllocs numAllocs = new NewOrderSingle.NoAllocs();
        numAllocs.set(new AllocAccount("AllocACC1"));
        numAllocs.set(new AllocShares(1010.10));
        message.addGroup(numAllocs);
        numAllocs.set(new AllocAccount("AllocACC2"));
        numAllocs.set(new AllocShares(2020.20));
        message.addGroup(numAllocs);
        return numAllocs;
    }

    public void testMessageSetGetString() {
        Message message = new Message();

        try {
            message.getString(5);
            assertTrue("exception not thrown", false);
        } catch (FieldNotFound e) {
        }

        message.setString(5, "string5");

        try {
            assertEquals("string5", message.getString(5));
        } catch (FieldNotFound e) {
            assertTrue("exception thrown", false);
        }

        try {
            message.setString(100, null);
            assertTrue("exception not thrown", false);
        } catch (NullPointerException e) {
        }
    }

    public void testMessagesetGetBoolean() {
        Message message = new Message();

        try {
            message.getBoolean(7);
            assertTrue("exception not thrown", false);
        } catch (FieldNotFound e) {
        }

        message.setBoolean(7, true);

        try {
            assertEquals(true, message.getBoolean(7));
        } catch (FieldNotFound e) {
            assertTrue("exception thrown", false);
        }
    }

    public void testMessageSetGetChar() {
        Message message = new Message();

        try {
            message.getChar(12);
            assertTrue("exception not thrown", false);
        } catch (FieldNotFound e) {
        }

        message.setChar(12, 'a');

        try {
            assertEquals('a', message.getChar(12));
        } catch (FieldNotFound e) {
            assertTrue("exception thrown", false);
        }
    }

    public void testMessageSetGetInt() {
        Message message = new Message();

        try {
            message.getInt(56);
            assertTrue("exception not thrown", false);
        } catch (FieldNotFound e) {
        }

        message.setInt(56, 23);

        try {
            assertEquals(23, message.getInt(56));
        } catch (FieldNotFound e) {
            assertTrue("exception thrown", false);
        }
    }

    public void testMessageSetGetDouble() {
        Message message = new Message();

        try {
            message.getDouble(9812);
            assertTrue("exception not thrown", false);
        } catch (FieldNotFound e) {
        }

        message.setDouble(9812, 12.3443);

        try {
            assertEquals(new Double(12.3443), new Double(message.getDouble(9812)));
        } catch (FieldNotFound e) {
            assertTrue("exception thrown", false);
        }
    }

    public void testMessageSetGetUtcTimeStamp() {
        Message message = new Message();

        try {
            message.getUtcTimeStamp(8);
            assertTrue("exception not thrown", false);
        } catch (FieldNotFound e) {
        }

        TimeZone timezone = TimeZone.getTimeZone("GMT+0");
        Calendar calendar = Calendar.getInstance(timezone);
        calendar.set(2002, 8, 6, 12, 34, 56);
        calendar.set(Calendar.MILLISECOND, 0);

        Date time = calendar.getTime();
        message.setUtcTimeStamp(8, time);

        try {
            assertEquals(message.getUtcTimeStamp(8).getTime(), time.getTime());
        } catch (FieldNotFound e) {
            assertTrue("exception thrown", false);
        }
    }

    public void testRemoveField() {
        Message message = new Message();
        message.setField(new StringField(12, "value"));
        assertTrue(message.isSetField(12));
        message.removeField(12);
        assertTrue(!message.isSetField(12));
    }

    public void testMessageIterator() {
        Message message = new Message();
        java.util.Iterator i = message.iterator();
        assertEquals(false, i.hasNext());
        try {
            assertNull(i.next());
            fail("exception not thrown");
        } catch (java.util.NoSuchElementException e) {
        }

        try {
            message = new Message("8=FIX.4.2\0019=12\00135=A\001108=30\00110=026\001");
            i = message.iterator();
            assertTrue(i.hasNext());
            StringField field = (StringField) i.next();
            assertEquals(108, field.getField());
            assertEquals("30", field.getValue());

            assertEquals(false, i.hasNext());
            try {
                assertNull(i.next());
                fail("exception not thrown");
            } catch (java.util.NoSuchElementException e) {
            }

            java.util.Iterator j = message.getHeader().iterator();
            assertTrue(j.hasNext());
            field = (StringField) j.next();
            assertEquals(8, field.getField());
            assertEquals("FIX.4.2", field.getValue());
            field = (StringField) j.next();
            assertEquals(9, field.getField());
            assertEquals("12", field.getValue());
            field = (StringField) j.next();
            assertEquals(35, field.getField());
            assertEquals("A", field.getValue());

            assertEquals(false, j.hasNext());
            try {
                assertNull(j.next());
                fail("exception not thrown");
            } catch (java.util.NoSuchElementException e) {
            }

        } catch (InvalidMessage e) {
            fail("exception thrown");
        }
    }

    public void testIsAdmin() {
        Message message = new Message();

        message.getHeader().setString(MsgType.FIELD, MsgType.HEARTBEAT);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.LOGON);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.LOGOUT);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.SEQUENCE_RESET);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.RESEND_REQUEST);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.TEST_REQUEST);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.REJECT);
        assertTrue(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.ORDER_SINGLE);
        assertFalse(message.isAdmin());

        message.getHeader().setString(MsgType.FIELD, MsgType.QUOTE_RESPONSE);
        assertFalse(message.isAdmin());
    }

    public void testComponent() throws Exception {
        Instrument instrument = new Instrument();
        instrument.set(new Symbol("DELL"));
        instrument.set(new CountryOfIssue("USA"));
        instrument.set(new SecurityType(SecurityType.COMMON_STOCK));

        quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle();
        newOrderSingle.set(instrument);
        newOrderSingle.set(new OrderQty(100));
        newOrderSingle.set(new Price(45));

        assertEquals(new Symbol("DELL"), newOrderSingle.getSymbol());
        assertEquals(new CountryOfIssue("USA"), newOrderSingle.getCountryOfIssue());
        assertEquals(new SecurityType(SecurityType.COMMON_STOCK), newOrderSingle.getSecurityType());

        newOrderSingle.set(new ClOrdID("CLIENT_ORDER_ID"));
        Instrument instrument2 = newOrderSingle.getInstrument();
        assertEquals(new Symbol("DELL"), instrument2.getSymbol());
        assertEquals(new CountryOfIssue("USA"), instrument2.getCountryOfIssue());
        assertEquals(new SecurityType(SecurityType.COMMON_STOCK), instrument2.getSecurityType());
        try {
            instrument2.getField(new ClOrdID());
            fail("should have thrown exception");
        } catch (FieldNotFound e) {
            // expected
        }

    }
}