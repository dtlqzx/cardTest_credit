/**
 *
 */
package helloworld;

import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.APDU;
import javacard.framework.Util;
/**
 * @author Administrator
 *
 */
public class HELLOWORLD extends Applet {
	private byte[] echoBytes;
	private static final short LENGTH_ECHO_BYTES=256;
	final static byte INITIALIZE_INS = (byte)0x50;//初始化
	final static byte PURCHASE_INS = (byte)0x54;//消费
	final static byte CREDIT_INS = (byte)0x56;//充钱
	final static byte GET_BALANCE_INS = (byte)0x5c;//查询

	short Balance;//余额
	short Value_Purchase;//交易金额

	protected HELLOWORLD(){
		echoBytes = new byte[LENGTH_ECHO_BYTES];
		register();
	}
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new HELLOWORLD();
	}

	public void process(APDU apdu) {
		byte buffer[]=apdu.getBuffer();

		short bytesRead=apdu.setIncomingAndReceive();
		short echoOffset = (short)0;
		switch(buffer[ISO7816.OFFSET_INS])
		{
			case INITIALIZE_INS:
				{initial(apdu); return;}//80 50 00 00
			case PURCHASE_INS:
				{purchase(apdu);return;}//80 54 00 00 02 xx xx
			case GET_BALANCE_INS:
				{getBalance(apdu);return;}//80 5c 00 00
			case CREDIT_INS:
				{credit(apdu);return;}//80 56 00 00 01 xx
			default:ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

//		while(bytesRead>0){
//			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, echoBytes, echoOffset, bytesRead);
//			echoOffset += bytesRead;
//			bytesRead = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
//		}
//		apdu.setOutgoing();
//		apdu.setOutgoingLength((short)(echoOffset+5));
//		apdu.sendBytes((short)0,(short)5);
//		apdu.sendBytesLong(echoBytes, (short)0, echoOffset);
	}
	private void initial(APDU apdu)
	{
		byte buffer[]=apdu.getBuffer();
		Value_Purchase = (short)Util.getShort(buffer, (short)8);

		apdu.setOutgoingAndSend((short)0, (short)15);
	}
	private void purchase(APDU apdu)
	{
		byte buffer[]=apdu.getBuffer();
		Value_Purchase = (short)Util.getShort(buffer, (short)5);
		Balance = (short)(Balance - Value_Purchase);

		apdu.setOutgoingAndSend((short)0, (short)8);
	}
	private void getBalance(APDU apdu)
	{
		byte buffer[]=apdu.getBuffer();
		Util.setShort(buffer, (short)0, (short)0);
		Util.setShort(buffer, (short)2, (short)Balance);

		apdu.setOutgoingAndSend((short)0, (short)4);
	}
	private void credit(APDU apdu)
	{
		byte buffer[]=apdu.getBuffer();
		byte creditAmount = buffer[ISO7816.OFFSET_CDATA];
		Balance = (short)(Balance + creditAmount);
	}
}
