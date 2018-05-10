package helloworld;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

public class HELLOWORLD extends Applet {
	final static byte wallet_ClA=(byte)0x80;
	final static byte VERIFY=(byte)0x20;
	final static byte CREDIT=(byte)0x30;
	final static byte DEBIT=(byte)0x40;
	final static byte GET_BALANCE=(byte)0x50;
	final static short MAX_BALANCE=0x7FFF;
	final static byte MAX_TRANSACTION_AMOUNT=127;
	final static byte PIN_TRY_LIMIT=(byte)0x03;
	final static byte MAX_PIN_SIZE=(byte)0x08;
	final static short SW_VERIFICATION_FAILED=0x6300;
	final static short SW_PIN_VERIFICATION_REQUIRIED=0x6301;
	final static short SW_INVALID_TRANSACTION_AMOUNT=0x6A83;
	final static short SW_EXCEED_MAXIMUM_BALANCE=0x6A84;
	final static short SW_NEGATIVE_BALANCE=0x6A85;
	OwnerPIN pin;
	short balance;
	private HELLOWORLD(byte[] bArray, short bOffset, byte bLength) {
		pin =new OwnerPIN(PIN_TRY_LIMIT,MAX_PIN_SIZE);
		byte iLen=bArray[bOffset];
		bOffset=(short)(bOffset+iLen+1);
		byte cLen = bArray[bOffset];
		bOffset = (short)(bOffset + cLen + 1);
		byte aLen = bArray[bOffset];
		pin.update(bArray, (short)(bOffset + 1), aLen);
		register();
	}


	public static void install(byte[] bArray, short bOffset, byte bLength) {
		new HELLOWORLD(bArray,bOffset, bLength);
	}

	public boolean select(){
		if(pin.getTriesRemaining()==0)
			return false;
		return true;
	}
	public void deselect(){
		pin.reset();
	}
	public void process(APDU apdu){
		byte[] buffer=apdu.getBuffer();
		buffer[ISO7816.OFFSET_CLA]=(byte)(buffer[ISO7816.OFFSET_CLA]&(byte)0xFC);
		if((buffer[ISO7816.OFFSET_CLA]==0)&&
			(buffer[ISO7816.OFFSET_INS]==(byte)(0xA4)))
			return;

		if(buffer[ISO7816.OFFSET_CLA]!= wallet_ClA)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		switch(buffer[ISO7816.OFFSET_INS]){
		case GET_BALANCE:///send 805000000002
			getBalance(apdu);
			return;

		case DEBIT:///send 8040000001037f
			debit(apdu);
			return;

		case CREDIT:///send 8030000001057f
			credit(apdu);
				return;
		case VERIFY://send 80200000040102030400
			verify(apdu);
			return;
		default:
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
	}

	private void getBalance(APDU apdu){
		byte[] buffer=apdu.getBuffer();
		short le=apdu.setOutgoing();
		if(le<2)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		apdu.setOutgoingLength((byte)2);

		buffer[0]=(byte)(balance>>8);
		buffer[1]=(byte)(balance&0xFF);
		apdu.sendBytes((short)0,(short)2);
	}
	private void credit (APDU apdu){
		if(!pin.isValidated())
				ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRIED);
		byte[]buffer = apdu.getBuffer();
		byte numBytes = buffer[ISO7816.OFFSET_LC];
		byte byteRead = (byte)(apdu.setIncomingAndReceive());
		if((numBytes != 1)||(byteRead != 1))
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		byte creditAmount = buffer[ISO7816.OFFSET_CDATA];
		if((creditAmount > MAX_TRANSACTION_AMOUNT)||(creditAmount < 0))
			ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
		if((short)(balance+creditAmount)>MAX_BALANCE)
			ISOException.throwIt(SW_EXCEED_MAXIMUM_BALANCE);
		balance = (short)(balance+creditAmount);
		}

	private void debit(APDU apdu){
		if(!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRIED);
		byte[]buffer = apdu.getBuffer();
		byte numBytes = (byte)(buffer[ISO7816.OFFSET_LC]);
		byte byteRead = (byte)(apdu.setIncomingAndReceive());
		if((numBytes != 1)||(byteRead != 1))
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		byte debitAmount = buffer[ISO7816.OFFSET_CDATA];
		if((debitAmount > MAX_TRANSACTION_AMOUNT)||(debitAmount < 0))
				ISOException.throwIt(SW_NEGATIVE_BALANCE);
		balance = (short)(balance - debitAmount);

	}

	private void verify(APDU apdu){
		byte[] buffer = apdu.getBuffer();

		byte byteRead = (byte)(apdu.setIncomingAndReceive());

		if(pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false){
			ISOException.throwIt(SW_VERIFICATION_FAILED);
		}
	}
	}
