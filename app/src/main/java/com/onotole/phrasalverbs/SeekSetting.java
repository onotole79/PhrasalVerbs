package com.onotole.phrasalverbs;

import android.widget.SeekBar;
import android.widget.TextView;

import static com.onotole.phrasalverbs.MainActivity.clSettings;

class SeekSetting {
    private SeekBar seekBar;
    int value = 1;
    private TextView textView;

    SeekSetting(int idSeekBar, int idTextView){
        this.seekBar = clSettings.findViewById(idSeekBar);
        this.textView = clSettings.findViewById(idTextView);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getMax() == 10){ // у слайдеров для шрифтов макс=10
                    value = progress+10;    //размер шрифта начинаем с 10
                }else {
                    value = progress+1;
                }
                showValue();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    void setPosition(){
        if (seekBar.getMax() == 10){ // у слайдеров для шрифтов макс=10
            seekBar.setProgress(value-10);
        }else {
            seekBar.setProgress(value-1);
        }
        showValue();
    }

    void showValue(){
        textView.setText(String.valueOf(value));
    }


}
