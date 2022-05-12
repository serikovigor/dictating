package ru.serikovigor.dictation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

//import com.serikovigor.dictation.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private static final int RESET_COUNTER = 20;
    private TextView textViewTest, textSents;
    private Button button, buttonPlay, buttonStop;
    public EditText editTranslate, editWord;

    //private TextToSpeech mTTS,mTTSRUS;
    private boolean ttsBusy;
    public boolean updateUI = true;
    private int wordPos = 0; //played word position
    private int wordsArrSize = 0;
    private int sentPos = -1; //played sentence position
    private boolean wordsPlayFlag = false;
    public String currentSentence = null;
    public int resetBusyCounter=0;

    public boolean firstPlay = true;

    public  int counter=0;
    Handler handler;
    ArrayAdapter<String> adapter;

    Locale rusLocale =  new Locale("ru");


    ArrayList<String> words = new ArrayList<>();
    //Main structure
    public ArrayList<ArrayList<String>> wordsArray;

    public MyRunnableTTX myRunnableTTX,myRunnableTTXRUS;

    public Thread threadTTX,threadTTXRUS;

    public void loadWordsTEST() {
        ArrayList<String> word1 = null;
        wordsArray = new ArrayList<ArrayList<String>>(10);
        ///
        word1 = new ArrayList<String>();
        word1.add("Hello");
        word1.add("привет");
        word1.add("Hello, my dear friends!");
        word1.add("Привет, мои дорогие друзья!");
        wordsArray.add(word1);
        ///
        word1 = new ArrayList<String>();
        word1.add("Gist");
        word1.add("Суть");
        word1.add("For me, that's the gist of it.");
        word1.add("Для меня, в этом вся его суть.");
        wordsArray.add(word1);
        ///

    }

    public void loadWordsFromFile(Uri selectedfile,boolean debug) {
        //Read text from file

        wordsArray = new ArrayList<ArrayList<String>>(10);
        StringBuilder text = new StringBuilder();
        //

        try {

            if (debug) {
                loadWordsTEST();
                //return;
            }
            else {
                BufferedReader br;
                InputStream in = getContentResolver().openInputStream(selectedfile);
                br = new BufferedReader(new InputStreamReader(in));

                String line;
                ArrayList<String> wordList = null;

                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        if (wordList != null)
                            wordsArray.add(wordList);
                        wordList = new ArrayList<String>();
                        //a1.add(1);
                        continue;
                    }
                    wordList.add(line);
                }
                br.close();
            }//else

        }//try
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        ///
        wordsArrSize = wordsArray.size();
        words.clear();
        for (int i = 0; i < wordsArrSize; i++) {
            words.add(wordsArray.get(i).get(0));
        }
        ///
        newWord(0);


    }

    public void playWord(String word, String language){
        //Locale locale = new Locale("eng");
        int result;
        //if (language.startsWith("ru"))
        //result = mTTS.setLanguage(rusLocale);
        //else
        //result = mTTS.setLanguage(Locale.ENGLISH);
        ///
        //mTTS.speak(word, TextToSpeech.QUEUE_FLUSH, null);
    }
    //////////
    // checking after current sentence plaing is done
    public void getNextWord(){
        sentPos++;
        if (sentPos>= wordsArray.get(wordPos).size()){
            updateUI=true;
            sentPos=0;
            wordPos++;
            if (wordPos>= wordsArray.size())
                wordPos=0;
        }
        ///
        if (sentPos==0)
            updateUI();

    }

    ////////////////////////////////////
    public class MyRunnableTTX implements Runnable {

        public boolean speakinTTX = false;
        private TextToSpeech mTTSENG;
        private Locale locale;
        final int uttIdLen = 10;

        public MyRunnableTTX(Locale loc) {
            this.locale = loc;
        }
        public synchronized void playText(String sentence) {
            /*
            String uttId=null;
            if (sentence.length() > uttIdLen)
                uttId = sentence.substring(0, uttIdLen);
            else
                uttId = sentence;

             */
            currentSentence = sentence;
            resetBusyCounter=0;
            this.mTTSENG.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, sentence);;
            //System.out.println("PLAY PLAY -->>>: "+locale.toLanguageTag() + ":  "+ sentence);
            speakinTTX=true;
        }
        public synchronized boolean isBusy() {
            return (this.mTTSENG.isSpeaking() | speakinTTX);
        }

        @Override
        public void run() {
            mTTSENG = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR) {
                        mTTSENG.setLanguage(locale);
                        speakinTTX =false;
                        //System.out.println("onInit MyRunnableTTX:"+locale.toLanguageTag());
                        mTTSENG.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                //speakinTTX =true;
                                //System.out.println("START------------->: "+locale.toLanguageTag() + utteranceId);
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                speakinTTX=false;
                                resetBusyCounter=0;
                                currentSentence=null;
                                //System.out.println("STOPP===========-============: "+locale.toLanguageTag() + utteranceId);
                            }

                            @Override
                            public void onError(String utteranceId) {
                                //System.out.println("ERROR: "+locale.toLanguageTag() + utteranceId);
                            }
                        });




                    }
                }
            });
            //////
            while(true) {
                //String s = isBusy() ? ". Busy" : "FREEE!!!";
                //System.out.println("TTX: "+locale.toLanguageTag() +s);

                try {
                    Thread.sleep(2L * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    ////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.common_menu, menu);


        return true;
    }
    ////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Операции для выбранного пункта меню
        switch (item.getItemId())
        {
            case R.id.open_new_words:
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    ////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        //
        myRunnableTTX = new MyRunnableTTX(Locale.ENGLISH);
        threadTTX = new Thread(myRunnableTTX);
        threadTTX.start();
        //
        myRunnableTTXRUS = new MyRunnableTTX(rusLocale);
        threadTTXRUS = new Thread(myRunnableTTXRUS);
        threadTTXRUS.start();
        //myRunnableTTXRUS.playText("Привет!");

        handler = new UpdateHandler();

        handler.sendEmptyMessageDelayed(1, 1000);//start after 1000
        //-------
        //EditText editTranslate = (EditText) findViewById(R.id.editTranslate);
        //editTranslate.setText("TRANSLATE");
        //editTranslate.setEnabled(false);
        //-----
        EditText editWord = (EditText) findViewById(R.id.editWordAdd);
        editWord.setText("WORD");
        editWord.setEnabled(false);
        //------
        textSents = (TextView) findViewById(R.id.textSents);
        //------
        // находим список
        ListView words_list = (ListView) findViewById(R.id.words_list);
        words_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        words_list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //String word = words[position];
                //playEnglish(word);
                newWord(position);
                //wordPos=position;
                //sentPos=0;
                updateUI();
                //Log.d(LOG_TAG, "itemClick: position = " + position + ", id = "
                //        + id);
            }
        });

        //------------------------------
        // создаем адаптер
        adapter = new ArrayAdapter<String>(this,
                R.layout.list_item, words);

        // присваиваем адаптер списку
        words_list.setAdapter(adapter);
        /////////
        //---------------------------
        buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (firstPlay) {
                    firstPlay = false;
                    Toast toast = Toast.makeText(getApplicationContext(),"The first launch will" +
                            " take 10 seconds to prepare",Toast.LENGTH_LONG);
                    toast.show();
                }
                if (wordsPlayFlag) {
                    wordsPlayFlag=false;
                    buttonPlay.setText("PLAY");
                }
                else {
                    wordsPlayFlag=true;
                    buttonPlay.setText("STOP");
                }
            }
        });
        ///////////
        ////////////////////////
        loadWordsFromFile(null, true);
        newWord(0);


    }//onCreate
    ///
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
    ///////////////////////////
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            loadWordsFromFile(selectedfile,false);
            updateUI();
        }
    }
    /////////////////////////////////
    private void newWord(int pos){
        sentPos=-1;
        wordPos=pos;

    }
    /////
    private void showCurrentWord(){
        if (wordPos>= wordsArrSize)
            return;
        ////

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordsArray.get(wordPos).size(); i++) {
            sb.append(wordsArray.get(wordPos).get(i) + "\n");
        }
        ///
        EditText editWord = findViewById(R.id.editWordAdd);
        editWord.setText(words.get(wordPos));
        //
        TextView textSents= findViewById(R.id.textSents);
        textSents.setText(sb.toString());
    }


    /////////////////////////////////
    private void updateUI() {
        //wordsNum=0; sentPos=0;

        adapter.notifyDataSetChanged();
        showCurrentWord();
        updateUI=false;
        ListView words_list = findViewById(R.id.words_list);
        words_list.clearFocus();
        words_list.requestFocusFromTouch();
        words_list.setSelection(wordPos);
    }
    ///////////////////////////
    public boolean ttxBusy(){
        return ( (myRunnableTTX.isBusy()) | (myRunnableTTXRUS.isBusy())   );

    }
    ///////////////////////////
    public void nextSentPLay(){
        if (ttxBusy()) {
            resetBusyCounter++;
            if (resetBusyCounter > RESET_COUNTER) {
                resetBusyCounter=0;
                //speakinTTX
                myRunnableTTX.speakinTTX=false;
                myRunnableTTXRUS.speakinTTX=false;

            }
            return;
        }
        ///////
        getNextWord();
        if ((sentPos % 2)==0) {
            myRunnableTTX.playText(wordsArray.get(wordPos).get(sentPos));
        }
        else {
            myRunnableTTXRUS.playText(wordsArray.get(wordPos).get(sentPos));
        }
        //////


    }
    ////
    class UpdateHandler extends Handler{
        private boolean startUP=true;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (updateUI)
                        updateUI() ;
                    //
                    if (wordsPlayFlag)
                        nextSentPLay();

                    ///
                    sendEmptyMessageDelayed(1, 500); //seng again after 1000
                    break;

                default:
                    break;
            }

        }

    }



}