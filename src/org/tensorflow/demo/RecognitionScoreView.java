/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;


import org.tensorflow.demo.Classifier.Recognition;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class RecognitionScoreView extends View implements ResultsView {
  private static final float TEXT_SIZE_DIP = 24;
  private List<Recognition> results;
  private final float textSizePx;
  private final Paint fgPaint;
  private final Paint bgPaint;
  private String objetoidentificado;

  public RecognitionScoreView(final Context context, final AttributeSet set) {
    super(context, set);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    fgPaint = new Paint();
    fgPaint.setTextSize(textSizePx);

    bgPaint = new Paint();
    bgPaint.setColor(0xcc4285f4);
}

  @Override
  public void setResults(final List<Recognition> results) {
    this.results = results;
    postInvalidate();

  }
// COMECO DO ENVIA DADOS
// http://seguranca.mybluemix.net/monitoramentoappandroidget?texto=dlp

    /*
    public void enviadados(){//ESTA COMPILANDO MAS NAO CONSEGUE MANDAR A REQUISICAO PARA A INTERNET
        String url = "http://seguranca.mybluemix.net/monitoramentoappandroidget?texto=dlp";
        URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.getInputStream();
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/




// FIM DO ENVIA DADOS

  @Override
  public void onDraw(final Canvas canvas) {
    final int x = 10;
    int y = (int) (fgPaint.getTextSize() * 1.5f);

    canvas.drawPaint(bgPaint);



    if (results != null) {

        for (final Recognition recog : results) {
        //canvas.drawText(recog.getTitle() + ": " + recog.getConfidence(), x, y, fgPaint);
        objetoidentificado = recog.getTitle().toString();
            if(objetoidentificado.equals("screen")||objetoidentificado.equals("notebook")||objetoidentificado.equals("laptop")||objetoidentificado.equals("monitor")||objetoidentificado.equals("desktop computer")||objetoidentificado.equals("mouse")||objetoidentificado.equals("computer keyboard")||objetoidentificado.equals("web site")){
              canvas.drawText("Alerta de vazamento de dados.", x, y, fgPaint);
                bgPaint.setColor(0xffff0000);

              //enviadados();



            }else{
              //canvas.drawText("De boa nada, tem que escrever o texto inteiro.", x, y, fgPaint);
              canvas.drawText(objetoidentificado, x, y, fgPaint);
                bgPaint.setColor(0xcc4285f4);

            }

        y += fgPaint.getTextSize() * 1.5f;
      }
    }
  }

}
