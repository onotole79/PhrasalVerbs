package com.onotole.phrasalverbs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.preference.PreferenceManager;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yandex.mobile.ads.AdEventListener;
import com.yandex.mobile.ads.AdRequest;
import com.yandex.mobile.ads.AdSize;
import com.yandex.mobile.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;



import static com.onotole.phrasalverbs.C.shift;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, DownloadDictionary.MainActivityMethods {
    SharedPreferences mPreferences;

    Base base;

    int mColumns, mVerbsCount;
    int mCurrentID = -1, mCurrentTable = 0;
    float mPreviousX;

    TableLayout table;
    TableRow.LayoutParams mButtonParams;
    TableRow rowTitle;
    TextView title;

    AlertDialog.Builder settingsDialog, searchDialog;
    static ConstraintLayout clSettings;
    CheckBox cbSoundOnOff;
    View searchDialogView;


    ArrayList<Button> buttonArrayList = new ArrayList<>();
    Button mCurrentButton = null;
    Button btSettings, btCards, btSearch;

    ArrayList<Card> cardList = new ArrayList<>();
    ArrayAdapter<Card> cardListAdapter;
    boolean[] cardChecked = new boolean[50];

    List<Variation> prepositionListElements = new ArrayList<>();
    ArrayAdapter<Variation> prepositionListAdapter;
    boolean[] mMeaningExamplesArray = new boolean[50];
    boolean isSearching;

    SoundPool mSoundPool;
    int mSoundButton, mSoundList;
    boolean mSoundTrigger = true;

    SeekSetting seekRows, seekCards, seekVerb, seekText;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // инициализируем рекламу
        AdView mAdView = findViewById(R.id.adView);
        mAdView.setBlockId(C.YANDEX_BLOCK_ID);
        mAdView.setAdSize(AdSize.BANNER_320x50);
        // Создание объекта таргетирования рекламы.
        AdRequest adRequest = AdRequest.builder().build();
        // Регистрация слушателя для отслеживания событий, происходящих в баннерной рекламе.
        mAdView.setAdEventListener(new AdEventListener.SimpleAdEventListener() {
            @Override
            public void onAdLoaded() {
            }
        });
        // Загрузка объявления.
        mAdView.loadAd(adRequest);






        // виджеты диалога установок
        clSettings = (ConstraintLayout) getLayoutInflater().inflate(R.layout.setting_dialog, null);
        cbSoundOnOff = clSettings.findViewById(R.id.cbSoundOnOff);

        seekRows = new SeekSetting(R.id.seekRows, R.id.tvRows);
        seekCards = new SeekSetting(R.id.seekCards, R.id.tvCards);
        seekVerb = new SeekSetting(R.id.seekVerb, R.id.tvVerb);
        seekText = new SeekSetting(R.id.seekText, R.id.tvText);

        // сам диалог
        settingsDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.settings))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mSoundTrigger = cbSoundOnOff.isChecked();
                        createColumnTable();
                        btSettings.setBackgroundResource(R.drawable.button_up);
                    }
                });
        settingsDialog.create();

        //диалог поиска
        searchDialogView = getLayoutInflater().inflate(R.layout.dialog_edittext, null);

        searchDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.search_st))
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        btSearch.setBackgroundResource(R.drawable.button_up);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditText editText = searchDialogView.findViewById(R.id.edit_text);
                        String searchingText = editText.getText().toString();
                        if (!searchingText.isEmpty()){
                            getFoundResults(searchingText);
                        }
                        btSearch.setBackgroundResource(R.drawable.button_up);
                    }
                });

        settingsDialog.create();

        setSounds();

        loadSettings();

        start ();

        Permissions permission = new Permissions(this);
        if (permission.AskPermission(new String[]{Manifest.permission.INTERNET})){
            checkNewDictionary();
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    public void start (){
        //расчитываем кол-во столбцов в таблице:
        //определяем максимально возможное кол-во колонок по ширине кнопок
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Button button = new Button(this);
        int buttonWidth = button.getMinimumWidth();
        int buttonHeight = button.getMinHeight();
        mColumns = displayMetrics.widthPixels/buttonWidth;

        // создаём параметры кнопок
        mButtonParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT);
        mButtonParams.gravity = Gravity.CENTER;
        //делаем высоту кнопок на 2/3 меньше
        mButtonParams.height = buttonHeight/2;

        //открываем словарь фразовых глаголов
        base = openDictionary();
        if (base.Meanings > 0){

            // Создаём кнопки на каждый глагол
            mVerbsCount = base.VerbsInfo.size();
            if (buttonArrayList.size() > 0){buttonArrayList.clear();}
            for (int i = 0; i< mVerbsCount; i++){
                Button btVerb = new Button(this);
                btVerb.setId(i);                      //каждой кнопке присраиваем ID, равный порядковому номеру
                btVerb.setBackgroundResource(R.drawable.button_up);
                btVerb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                btVerb.setTypeface(Typeface.SERIF, Typeface.BOLD);
                btVerb.setPadding(shift,-shift,-shift,shift);   //выравним текст, если он уходит за края
                btVerb.setText(base.VerbsInfo.get(i).Verb);     //берём из базы данные о глаголе имя глагола
                btVerb.setOnClickListener(this);                //ставим слушатель на каждую кнопку

                btVerb.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return touchListener(view, motionEvent);
                    }
                });


                buttonArrayList.add(btVerb);
            }

            //создаём таблицу
            createTable();

            //создаём пустой список
            createList();

            //адаптер для проверочных карточек
            createCardAdapter();
       }
    }



    //обработчик кнопок
    @Override
    public void onClick(View v)
    {
        cardList.clear();
        prepositionListElements.clear();

        if (mCurrentID != -1){
            // восстанавливаем предыдущую кнопку
            mCurrentButton.setTextColor(Color.BLACK);
            mCurrentButton.setBackgroundResource(R.drawable.button_up);
            mCurrentButton.setPadding(shift,-shift,-shift,shift);  //поднимаем текст(как будто кнопка отпущена)
        }


        if (v == btSettings) {
            btSettings.setBackgroundResource(R.drawable.button_down);

            cbSoundOnOff.setChecked(mSoundTrigger);

            seekRows.setPosition();
            seekCards.setPosition();
            seekVerb.setPosition();
            seekText.setPosition();

            if (clSettings.getParent() != null) {    //удаляем установленное View
                ((ViewGroup) clSettings.getParent()).removeView(clSettings);
            }
            // и заново ставим (иначе будет лшибка)
            settingsDialog.setView(clSettings);
            settingsDialog.show();


        }else if(v == btSearch) {
            btSearch.setBackgroundResource(R.drawable.button_down);
            if (searchDialogView.getParent() != null) {    //удаляем установленное View
                ((ViewGroup) searchDialogView.getParent()).removeView(searchDialogView);
            }// и заново ставим (иначе будет лшибка)
            searchDialog.setView(searchDialogView);
            searchDialog.show();

        }else if(v == btCards) {
            checkYourSelf();

        }else{
            if (mCurrentID == v.getId()){
                mCurrentID = -1;

            }else {
                mCurrentID = v.getId();
                //на какой глагол нажали
                VerbInfo verbInfo = base.VerbsInfo.get(mCurrentID);
                for(int i=0; i < verbInfo.Variations.size(); i++){
                    prepositionListElements.add(i, verbInfo.Variations.get(i)); //добавляем элементы для list
                    mMeaningExamplesArray[i] = false;
                }
                prepositionListAdapter.notifyDataSetChanged();
                mCurrentButton = (Button)v;
                mCurrentButton.setTextColor(Color.GREEN);
                mCurrentButton.setBackgroundResource(R.drawable.button_down);
                mCurrentButton.setPadding(-shift,shift,shift,shift);  //опускаем текст(как будто кнопка нажата)
            }
            if (mSoundTrigger){
                mSoundPool.play(mSoundButton, 1, 1, 1, 0,1);
            }
        }
    }



    private void createList() {
        prepositionListAdapter = new ArrayAdapter<Variation>             //создаём адаптер для списков
                (MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, prepositionListElements){
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setBackgroundResource(R.drawable.list_meaning);

                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setText(prepositionListElements.get(position).Preposition);
                text1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, seekVerb.value);
                text1.setTypeface(Typeface.SERIF ,Typeface.BOLD);

                TextView text2 = view.findViewById(android.R.id.text2);
                text2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, seekText.value);
                showTextView2(view, text2, position);
                return view;
            }
        };

        ListView prepositionListView = new ListView(this);
        prepositionListView.setAdapter(prepositionListAdapter);     //применяем адаптер к ListView
        prepositionListView.setDividerHeight(3);
        table.addView(prepositionListView);                         //прикрепляем к нашей таблице ListView

        //ставим слушатель нажатий на список
        prepositionListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView text2 = view.findViewById(android.R.id.text2);
                mMeaningExamplesArray[position]^=true;
                showTextView2(view, text2, position);
                if (mSoundTrigger){
                    mSoundPool.play(mSoundList, 1, 1, 1, 0,1);
                }
            }
        });
    }

    void showTextView2(View view, TextView textView, int position){
        if (mMeaningExamplesArray[position]){
            view.setBackgroundResource(R.drawable.list_example);
            // чтобы снизу было ещё пустое место
            String str = prepositionListElements.get(position).Example + "\n ";
            textView.setText(str);
        }
        else {
            view.setBackgroundResource(R.drawable.list_meaning);
            // чтобы снизу было ещё пустое место
            String str = prepositionListElements.get(position).Meaning + "\n ";
            textView.setText(str);
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private void createTable(){
        table = new TableLayout(this);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);
        createTitleTable();
        createColumnTable();

        ConstraintLayout layout_table = findViewById(R.id.layout_table);
       // layout_table.setBackgroundResource(R.drawable.layers_background);
        layout_table.addView(table);

        // обработка перемещений глаголов касанием по фону
        layout_table.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return touchListener(view, motionEvent);
            }
        });
    }

    private boolean touchListener(View view, MotionEvent motionEvent){
        float way = 0;
        float way_abs = 0;
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN: // нажатие
                mPreviousX = motionEvent.getX();
                break;
            case MotionEvent.ACTION_UP: // отпускание
                way = motionEvent.getX() - mPreviousX;
                way_abs = Math.abs(way);
                if (way_abs < 100){   //если палец мало перемещался, то принмаем как за кнопку
                    view.performClick();
                }
                break;
        }
        if (way_abs > 100){    // если палец достаточно перемещался
            if (way < 0){
                int maxTables = (int)Math.ceil((float)mVerbsCount/(mColumns*seekRows.value));
                if (mCurrentTable < maxTables-1){
                    mCurrentTable++;
                    createColumnTable();
                }
            }else if(way > 0){
                if (mCurrentTable > 0){
                    mCurrentTable--;
                    createColumnTable();
                }
            }
        }
        return true;
    }


    private void createTitleTable(){
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = mColumns;

        rowTitle = new TableRow(this);     //первая строка таблицы
        rowTitle.setBackgroundColor(ContextCompat.getColor(this, R.color.colorGreen80d76e));
        rowTitle.setGravity(Gravity.CENTER);

        title = new TextView(this);        //создаём элемент текста
        String sTitle = String.format(Locale.US,"Фразовые глаголы v.%s(%d_%d)", BuildConfig.VERSION_NAME, base.VerbsInfo.size(), base.Meanings);
        title.setText(sTitle);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        rowTitle.addView(title, params);
        table.addView(rowTitle);


        rowTitle = new TableRow(this);     //вторая строка таблицы
        rowTitle.setBackgroundColor(ContextCompat.getColor(this, R.color.colorGreen80d76e));
        rowTitle.setGravity(Gravity.CENTER);

        // добавим строку с кнопками настроек, поиска и карточками

        btSettings = new Button(this);
        setButtonParams(btSettings, R.string.settings);
        btCards = new Button(this);
        setButtonParams(btCards, R.string.check);
        btSearch = new Button(this);
        setButtonParams(btSearch, R.string.search);

        /*
        _____________________________________
        |check|search|...|textview|settings|
         0-ой        mColumns-3     (Columns-1)-ый
        элемент   кол-во textview   элемент
        */

        rowTitle.addView(btCards, mButtonParams);    //добаваляем кнопку карточек
        rowTitle.addView(btSearch, mButtonParams);   //добаваляем кнопку поиска
        for (int i = 0; i < mColumns-3; i++){
            TextView textView = new TextView(this);
            rowTitle.addView(textView);
        }
        rowTitle.addView(btSettings, mButtonParams); //добаваляем кнопку настроек
        table.addView(rowTitle);                     //добавляем заголовок в таблицу
    }

    private void createColumnTable(){
        clearTableList();

        int verbsCount = mVerbsCount;
        int startVerb = mCurrentTable*seekRows.value*mColumns;
        verbsCount-= startVerb;
        for (int i = 0; i < seekRows.value; i++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setBackgroundResource(R.drawable.row_layout);

            for (int j = 0; j < mColumns; j++) {
                verbsCount--;
                if (verbsCount < 0) break;
                Button btVerb = buttonArrayList.get(startVerb + i* mColumns +j);
                tableRow.addView(btVerb, mButtonParams);   //добавляем кнопку в строку
            }
            table.addView(tableRow, i+2);     //добавляем строку в таблицу
            if (verbsCount < 0) break;              //+2 первый элемент - шапка, второй - строка с кнопками
        }                                           //check и settings
    }


    private void clearTableList(){
        for (int i = 0; i < mVerbsCount; i++) {
            Button btVerb = buttonArrayList.get(i);
            if (btVerb.getParent() != null){    // если кнопка была уже добавлена - удаляем её
                ((ViewGroup)btVerb.getParent()).removeView(btVerb);
            }
        }
    }




    private void createCardAdapter(){
        cardListAdapter = new ArrayAdapter<Card>             //создаём адаптер для списков
                (MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, cardList){
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setBackgroundResource(R.drawable.list_meaning);

                TextView text1 = view.findViewById(android.R.id.text1);
                String preposition = String.format(Locale.US,"%s %s", cardList.get(position).Verb, cardList.get(position).Preposition);
                text1.setText(preposition);
                text1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, seekVerb.value);
                text1.setTypeface(Typeface.SERIF ,Typeface.BOLD);

                TextView text2 = view.findViewById(android.R.id.text2);
                text2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, seekText.value);

                showCard(view, text2, position);
                return view;
            }
        };

        ListView cardsListView = new ListView(this);
        cardsListView.setAdapter(cardListAdapter);     //применяем адаптер к ListView
        table.addView(cardsListView);                  //прикрепляем к нашей таблице ListView

        //ставим слушатель на список
        cardsListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView text2 = view.findViewById(android.R.id.text2);
                cardChecked[position]^=true;
                showCard(view, text2, position);

                if (mSoundTrigger){
                    mSoundPool.play(mSoundList, 1, 1, 1, 0,1);
                }
            }
        });
    }

    void showCard(View view, TextView textView, int position){
        String str = "";
        switch ((cardChecked[position] ? 1:0) + (isSearching ? 1:0)){
            case 0:
                view.setBackgroundResource(R.drawable.list_example);
                break;
            case 1:
                view.setBackgroundResource(R.drawable.list_meaning);
                str = cardList.get(position).Meaning;
                break;
            case 2:
                view.setBackgroundResource(R.drawable.list_example);
                str = cardList.get(position).Example;
        }
        str+="\n";
        textView.setText(str);
    }




    private void checkYourSelf(){
        for (int i = 0; i< cardChecked.length;i++){cardChecked[i]=false;}
        isSearching = false;
        // создаём список случайных карточек
        Random rnd = new Random();
        for(int i = 0; i < seekCards.value; i++){
            // случайный глагол
            int randVerb = rnd.nextInt(mVerbsCount);
            VerbInfo verbInfo = base.VerbsInfo.get(randVerb);
            // случайная вариация
            int randPreposition = rnd.nextInt(verbInfo.Variations.size());
            Card card = new Card(verbInfo.Verb, verbInfo.Variations.get(randPreposition).Preposition, verbInfo.Variations.get(randPreposition).Meaning, verbInfo.Variations.get(randPreposition).Example);
            cardList.add(card);
        }
        cardListAdapter.notifyDataSetChanged();
    }


    private void getFoundResults(String searchingText){
        for (int i = 0; i< cardChecked.length;i++){cardChecked[i]=false;}
        isSearching = true;
        // перебираем все глаголы
        for(int v = 0; v < mVerbsCount; v++){
            VerbInfo verbInfo = base.VerbsInfo.get(v);
            // перебираем все значения
            for (int m = 0; m < verbInfo.Variations.size(); m++){
                String meaning = verbInfo.Variations.get(m).Meaning;
                if (meaning.toLowerCase().contains(searchingText.toLowerCase())){
                    Card card = new Card(verbInfo.Verb, verbInfo.Variations.get(m).Preposition, verbInfo.Variations.get(m).Meaning, verbInfo.Variations.get(m).Example);
                    cardList.add(card);
                }
            }
        }
        cardListAdapter.notifyDataSetChanged();
    }






    private void checkNewDictionary(){
        new DownloadDictionary(this, this).execute();
    }

    private Base openDictionary() {
        Base base = new Base();
        base.VerbsInfo = new ArrayList<>();

        String sDict = readFile(this.getFilesDir() + C.dictFileName);
        if (!sDict.isEmpty()){

            try {
                JSONArray dictionary = new JSONArray(sDict);

                for (int i = 0; i < dictionary.length();i++) {
                    JSONObject verbsJSON = dictionary.getJSONObject(i);

                    //берём название глагола
                    String verb = verbsJSON.getString("verb");

                    //теперь его список
                    JSONArray variationsJSON = verbsJSON.getJSONArray("variations");
                    List<Variation> variations = new ArrayList<>();
                    base.Meanings += variationsJSON.length();
                    for (int k=0; k < variationsJSON.length(); k++) {
                        JSONObject variationObj =  variationsJSON.getJSONObject(k);
                        Variation variation = new Variation();
                        variation.Preposition = variationObj.getString("preposition");
                        variation.Meaning = variationObj.getString("meaning");
                        variation.Example = variationObj.getString("example");
                        variations.add(variation);
                    }
                    VerbInfo verbInfo = new VerbInfo();
                    verbInfo.Verb = verb;
                    verbInfo.Variations = variations;
                    base.VerbsInfo.add(verbInfo);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                return base;
            }
        }
        return base;
    }


    private String readFile(String file) {
        StringBuilder stringBuilder = new StringBuilder();

        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }


    private void setSounds(){
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        SoundPool.Builder builder= new SoundPool.Builder();
        builder.setAudioAttributes(audioAttributes).setMaxStreams(10);
        mSoundPool = builder.build();
        mSoundButton = mSoundPool.load(this, R.raw.button_click,1);
        mSoundList = mSoundPool.load(this, R.raw.list_click,1);
    }

    private void setButtonParams(Button button, int idString){
        button.setBackgroundResource(R.drawable.button_up);
        button.setText(idString);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        button.setPadding(shift,-shift,-shift,shift);
        button.setOnClickListener(this);
    }







    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == C._requestCode) {
            //если хоть один доступ не разрешён - шлём запросы
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, C._requestCode);
                }
            }
            checkNewDictionary();   //скачиваем словарь когда доступ разрешили
        }
    }


    @Override
    public void onPause(){
        super.onPause();
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(C.SOUND_TRIGGER,mSoundTrigger);
        editor.putInt(C.ROWS, seekRows.value);
        editor.putInt(C.CARDS, seekCards.value);
        editor.putInt(C.VERB_SIZE, seekVerb.value);
        editor.putInt(C.TEXT_SIZE, seekText.value);
        editor.apply();
    }


    void loadSettings(){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSoundTrigger = mPreferences.getBoolean(C.SOUND_TRIGGER, true);
        seekRows.value = mPreferences.getInt(C.ROWS, 3);
        seekCards.value = mPreferences.getInt(C.CARDS, 10);
        seekVerb.value = mPreferences.getInt(C.VERB_SIZE, 14);
        seekText.value = mPreferences.getInt(C.TEXT_SIZE, 16);
    }


    @Override
    public void onResume(){
        super.onResume();
        loadSettings();
    }



}
