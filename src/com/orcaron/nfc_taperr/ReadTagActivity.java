package com.orcaron.nfc_taperr;

import com.parse.ParseUser;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
 

import android.app.PendingIntent;

import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class ReadTagActivity extends Activity {
	     public static final String MIME_TEXT_PLAIN = "text/plain";
	    public static final String TAG = "NfcDemo";
	 
	    private TextView mTextView;
	    private NfcAdapter mNfcAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_tag);
		
	     mTextView = (TextView) findViewById(R.id.readText);
	     
	        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
	 
	        if (mNfcAdapter == null) {
	            // Stop here, we definitely need NFC
	            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	 
	        }
	     
	        if (!mNfcAdapter.isEnabled()) {
	            mTextView.setText("NFC is disabled.");
	        } else {
	            mTextView.setText(R.string.read_tag_label);
	        }
	         
	        handleIntent(getIntent());

	
	}
	
	 @Override
	    protected void onResume() {
	        super.onResume();
	         
	        /**
	         * It's important, that the activity is in the foreground (resumed). Otherwise
	         * an IllegalStateException is thrown. 
	         */
	        setupForegroundDispatch(this, mNfcAdapter);
	    }
	     
	    @Override
	    protected void onPause() {
	        /**
	         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
	         */
	        stopForegroundDispatch(this, mNfcAdapter);
	         
	        super.onPause();
	    }
	     
	    @Override
	    protected void onNewIntent(Intent intent) { 
	        /**
	         * This method gets called, when a new Intent gets associated with the current activity instance.
	         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
	         * at the documentation.
	         * 
	         * In our case this method gets called, when the user attaches a Tag to the device.
	         */
	        handleIntent(intent);
	    }
	     
	    private void handleIntent(Intent intent) {
	        String action = intent.getAction();
	        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
	             
	            String type = intent.getType();
	            if (MIME_TEXT_PLAIN.equals(type)) {
	     
	                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	                new NdefReaderTask().execute(tag);
	                 
	            } else {
	                Log.d(TAG, "Wrong mime type: " + type);
	            }
	        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
	             
	            // In case we would still use the Tech Discovered Intent
	            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	            String[] techList = tag.getTechList();
	            String searchedTech = Ndef.class.getName();
	             
	            for (String tech : techList) {
	                if (searchedTech.equals(tech)) {
	                    new NdefReaderTask().execute(tag);
	                    break;
	                }
	            }
	        }
	    }
	     
	    /**
	     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
	     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
	     */
	    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
	        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
	        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	 
	        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
	 
	        IntentFilter[] filters = new IntentFilter[1];
	        String[][] techList = new String[][]{};
	 
	        // Notice that this is the same filter as in our manifest.
	        filters[0] = new IntentFilter();
	        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
	        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
	        try {
	            filters[0].addDataType(MIME_TEXT_PLAIN);
	        } catch (MalformedMimeTypeException e) {
	            throw new RuntimeException("Check your mime type.");
	        }
	         
	        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
	    }
	 
	    /**
	     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
	     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
	     */
	    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
	        adapter.disableForegroundDispatch(activity);
	    }
		private void navigateToSignin() {
			Intent mainintent = new Intent(this, SigninActivity.class);
			mainintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mainintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(mainintent);
		}
	    
		@Override
		public boolean onCreateOptionsMenu(Menu menu) {

			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.read_tag, menu);
			return true;
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			// Handle action bar item clicks here. The action bar will
			// automatically handle clicks on the Home/Up button, so long
			// as you specify a parent activity in AndroidManifest.xml.
			int itemId = item.getItemId();
			if (itemId == R.id.action_signout) {
				ParseUser.logOut();
				navigateToSignin();
			}
			return super.onOptionsItemSelected(item);
		}
	    
	    /**
	     * Background task for reading the data. Do not block the UI thread while reading. 
	     * 
	     * @author Ralf Wondratschek
	     *
	     */
	    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
	     
	        @Override
	        protected String doInBackground(Tag... params) {
	            Tag tag = params[0];
	             
	            Ndef ndef = Ndef.get(tag);
	            if (ndef == null) {
	                // NDEF is not supported by this Tag. 
	                return null;
	            }
	     
	            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
	     
	            NdefRecord[] records = ndefMessage.getRecords();
	            for (NdefRecord ndefRecord : records) {
	                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
	                    try {
	                        return readText(ndefRecord);
	                    } catch (UnsupportedEncodingException e) {
	                        Log.e(TAG, "Unsupported Encoding", e);
	                    }
	                }
	            }
	     
	            return null;
	        }
	         
	        private String readText(NdefRecord record) throws UnsupportedEncodingException {
	            /*
	             * See NFC forum specification for "Text Record Type Definition" at 3.2.1 
	             * 
	             * http://www.nfc-forum.org/specs/
	             * 
	             * bit_7 defines encoding
	             * bit_6 reserved for future use, must be 0
	             * bit_5..0 length of IANA language code
	             */
	     
	            byte[] payload = record.getPayload();
	     
	            // Get the Text Encoding
	            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
	     
	            // Get the Language Code
	            int languageCodeLength = payload[0] & 0063;
	             
	            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
	            // e.g. "en"
	             
	            // Get the Text
	            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
	        }
	         
	        @Override
	        protected void onPostExecute(String result) {
	            if (result != null) {
	                mTextView.setText("Read content: " + result);
	            }
	        }
	}
}