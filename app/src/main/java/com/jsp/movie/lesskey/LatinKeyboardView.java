/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jsp.movie.lesskey;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.List;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;

    Context cxt;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        cxt = context;
        init ();
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        cxt = context;
        init ();
    }

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		// TODO Auto-generated method stub
/*		
		int action = me.getAction();
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Toast toast = Toast.makeText(getContext(), "DOWN", Toast.LENGTH_LONG);
			toast.setDuration(toast.LENGTH_SHORT);
			toast.show();			
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_UP:
			break;
		}
		return true;
*/		
		return super.onTouchEvent(me);
	}


	
	@Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }

    Paint bg;
    Paint keyOn;
    Paint keyOff;
    Paint keyBg;
    Paint text;

    private void init () {

        bg = new Paint ();
        bg.setColor(getResources().getColor(android.R.color.background_light));
        keyOn = new Paint ();
        keyOn.setColor(getResources().getColor(android.R.color.holo_blue_bright));
        keyOn.setAntiAlias(true);
        keyOff = new Paint ();
        keyOff.setColor(getResources().getColor(android.R.color.holo_blue_dark));
        keyOff.setAntiAlias(true);
        keyBg = new Paint ();
        keyBg.setColor(getResources().getColor(android.R.color.holo_blue_light));
        keyBg.setAntiAlias(true);
        text = new Paint ();
//        text.setColor(getResources().getColor(android.R.color.primary_text_light));
        text.setColor(Color.BLACK);
        text.setAntiAlias(true);
        text.setTypeface(Typeface.create(Typeface.defaultFromStyle(android.R.style.DeviceDefault_Light_ButtonBar), Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void onDraw(Canvas canvas) {

        canvas.drawColor(cxt.getResources().getColor(android.R.color.background_light));

        List<Key> keys = getKeyboard().getKeys();

        for (Key key : keys) { // int i = 0 ; switch(i) and implement your logic

            int h = key.height;
            int m = h/6;
            int r = m;

            RectF rect = new RectF(key.x + m, key.y + m, key.x + key.width - m, key.y + key.height - m);

            text.setTextSize(2 * m);

            canvas.drawRoundRect(new RectF(key.x + m, key.y + m, key.x + key.width - m + r/2, key.y + key.height - m + r/2), r, r, keyBg);

            if (key.pressed || (key.codes[0] == -1 && getKeyboard().isShifted()))
                canvas.drawRoundRect(rect, r, r, keyOn);
            else
                canvas.drawRoundRect(rect, r, r, keyOff);

            if (key.label != null)
                canvas.drawText(key.label, 0, key.label.length(), key.x + key.width/2, key.y + h/2 + m, text);
            else if (key.icon != null) {

                int top = key.y + 3*m/2;
                int bottom = key.y + key.height - 3*m/2;

                int height = bottom - top;
                int width = height*key.icon.getMinimumWidth()/key.icon.getMinimumHeight();

                key.icon.setBounds(key.x + (key.width - width)/2, top, key.x + (key.width + width)/2, bottom);
                key.icon.draw(canvas);
            }
        }
    }
}
