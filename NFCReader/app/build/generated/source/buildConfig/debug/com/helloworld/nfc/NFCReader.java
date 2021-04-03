package com.helloworld.nfc;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.ndeftools.Message;
import org.ndeftools.Record;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class NFCReader extends Activity  {

	private static String TAG = NFCReader.class.getName();
	
	protected NfcAdapter nfcAdapter;
    protected PendingIntent nfcPendingIntent;
    private Tag tag;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate");
		setContentView(R.layout.main);
    	nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	Log.d(TAG, "onResume");
    	
    	enableForegroundMode();
    }
    
    @Override
    protected void onPause() { 
    	super.onPause();

    	Log.d(TAG, "onPause");

    	disableForegroundMode();
    }

	@SuppressLint("SetTextI18n")
	@Override
	public void onNewIntent(Intent intent) {	//questo metodo viene chiamato quando il lettore del dispositivo rileva un tag NFC.
		Log.d(TAG, "E' stato chiamato onNewIntent");

		/*PRIMO STEP: IL LETTORE NFC RILEVA LA PRESENZA DI UN TAG*/

		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
			|| NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())
			|| NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			Log.d(TAG, "New intent: ho rilevato un tag!");
			vibrate(); //la vibrazione indica lettura del tag

			tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			byte[] id = tag.getId();

			for (String tech : tag.getTechList()) {
				if (tech.equals(NfcV.class.getName())) {
					NfcV nfcvTag = NfcV.get(tag);
					try {
						// Connect to the NFC Tag
						nfcvTag.connect();
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), "Impossibile aprire una connesione", Toast.LENGTH_SHORT).show();
						return;
					}

					try {
						byte[] cmd = new byte[]{
								(byte) 0x20, // Flags
								(byte) 0x23, // Read Multiple Blocks
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00,
								(byte) 0x00, // Starting address
								(byte) 0x0F, // 16 blocks to read
						};

						System.arraycopy(tag.getId(), 0, cmd, 2, 8);

						byte[] response = nfcvTag.transceive(cmd);

						TextView textView = (TextView) findViewById(R.id.title);

						
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), "Errore durante la lettura", Toast.LENGTH_SHORT).show();
						return;
					}
				}
			}

			Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES); //raccolgo i raw messages dall'intent
			NdefMessage[] messages = null; //setto l'array di ndefmessages a null
			if (rawMessages != null) {
				Log.d(TAG, "Ci sono " + rawMessages.length + " messaggi NDEF");
				messages = new NdefMessage[rawMessages.length]; //setto messages come array di ndefmessages in base a quanti sono i raw letti
				for (int i = 0; i < rawMessages.length; i++) {
					messages[i] = (NdefMessage) rawMessages[i]; // per ogni raw message faccio il casting a ndef message nell'array message
				}
			}
			else {
				Log.d(TAG, "Non ho trovato messaggi");
			}
			NdefMessage ndefMessage = (NdefMessage)messages[0]; //casto il primo elemento di messages in un ndefMessage
			leggiMessaggi(messages);
			try {
				List<Record> records = new Message(ndefMessage); //analizziamo il ndefMessage tramite una lista di records
				Log.d(TAG, "Trovati " + records.size() + " record nel messaggio");
				for(int k = 0; k < records.size(); k++) {
					Record record = records.get(k);

					Log.d(TAG, " Il record #" + k + " è della classe " + record.getClass().getName());

				}
			} catch (Exception e) {
				Log.e(TAG, "Problema durante il parsing del messaggio", e);
			}


		}
	}

	private void leggiMessaggi(NdefMessage[] messages){
    	if(messages == null || messages.length == 0) return;

		String text = "";
		String tagId = new String(messages[0].getRecords()[0].getType()); //leggo ID tag
		byte[] payload = messages[0].getRecords()[0].getPayload();  //raccolgo il payload intero come array di byte
		String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; //legge la codifica dei caratteri
		int languageCodeLength = payload[0] & 0063; // ottengo il linguaggio del testo sotto forma di intero
		//String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII"); //linguaggio come stringa

		try {
			//Ottengo il testo vero e proprio
			text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
		} catch (UnsupportedEncodingException e) {
			Log.e("Codifica non supportata", e.toString());
		}
		TextView textView = (TextView) findViewById(R.id.title);
		textView.setText(text);
    }

	private void vibrate() {
		Log.d(TAG, "vibrate");

		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) ;
		vibe.vibrate(500);
	}

	 /**
     * Converts the byte array to HEX string.
     * 
     * @param buffer
     *            the buffer.
     * @return the HEX string.
     */
    public String toHexString(byte[] buffer) {
		StringBuilder sb = new StringBuilder();
		for(byte b: buffer)
			sb.append(String.format("%02x ", b&0xff));
		return sb.toString().toUpperCase();
    }

    public void enableForegroundMode(){
    	Log.d(TAG,"enableForegroundMode");
    	//La foreground mode setta la priorità di leggere tag all'applicazione attiva
		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
		nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
	}

	public void disableForegroundMode() {
		Log.d(TAG, "disableForegroundMode");
		nfcAdapter.disableForegroundDispatch(this);
	}
    
}