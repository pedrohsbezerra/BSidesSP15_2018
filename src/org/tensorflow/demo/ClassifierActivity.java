/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.List;
import java.util.Vector;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.R; // Explicit import needed for internal Google builds.

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  protected static final boolean SAVE_PREVIEW_BITMAP = false;

  private ResultsView resultsView;

  //INICIA A CONFIGURACAO DO ENVIA DADOS
  //
  private List<Classifier.Recognition> results2;
  private String objetoidentificado2;

  public void setResults(final List<Classifier.Recognition> results2) {
    this.results2 = results2;
  }


  // COMPILA, NAO DA ERRO, MAS NAO ACONTECE NADA

  public void agoravai(){
    /* // V1 COMPILA, NAO DA ERRO, MAS NAO ACONTECE NADA
    RequestQueue fila = Volley.newRequestQueue(this);
    fila.start();
    String url = "http://seguranca.mybluemix.net/monitoraappandroidget?texto=dlp";
    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
              @Override
              public void onResponse(String response) {

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {

      }
    });
    fila.add(stringRequest);
    */ //FIM V1

        RequestQueue mRequestQueue;

    // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

    // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

    // Start the queue
        mRequestQueue.start();

        String url ="http://seguranca.mybluemix.net/monitoramentoappandroidget?texto="+objetoidentificado2;

    // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                  @Override
                  public void onResponse(String response) {
                    // Do something with the response
                  }
                },
                new Response.ErrorListener() {
                  @Override
                  public void onErrorResponse(VolleyError error) {
                    // Handle error
                  }
                });

    // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);

  }

  //FIM DO ENVIA DADOS




  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private long lastProcessingTimeMs;



  // These are the settings for the original v1 Inception model. If you want to
  // use a model that's been produced from the TensorFlow for Poets codelab,
  // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
  // INPUT_NAME = "Mul", and OUTPUT_NAME = "final_result".
  // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
  // the ones you produced.
  //
  // To use v3 Inception model, strip the DecodeJpeg Op from your retrained
  // model first:
  //
  // python strip_unused.py \
  // --input_graph=<retrained-pb-file> \
  // --output_graph=<your-stripped-pb-file> \
  // --input_node_names="Mul" \
  // --output_node_names="final_result" \
  // --input_binary=true
  private static final int INPUT_SIZE = 224;
  private static final int IMAGE_MEAN = 117;
  private static final float IMAGE_STD = 1;
  private static final String INPUT_NAME = "input";
  private static final String OUTPUT_NAME = "output";


  private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
  private static final String LABEL_FILE =
      "file:///android_asset/imagenet_comp_graph_label_strings.txt";


  private static final boolean MAINTAIN_ASPECT = true;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);


  private Integer sensorOrientation;
  private Classifier classifier;
  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;


  private BorderedText borderedText;




  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  private static final float TEXT_SIZE_DIP = 10;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    classifier =
        TensorFlowImageClassifier.create(
            getAssets(),
            MODEL_FILE,
            LABEL_FILE,
            INPUT_SIZE,
            IMAGE_MEAN,
            IMAGE_STD,
            INPUT_NAME,
            OUTPUT_NAME);

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

    frameToCropTransform = ImageUtils.getTransformationMatrix(
        previewWidth, previewHeight,
        INPUT_SIZE, INPUT_SIZE,
        sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            renderDebug(canvas);
          }
        });
  }

  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }
    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);

            // INICIO DO ARRAY DE IMAGENS IDENTIFICADAS
            for (final Classifier.Recognition recog : results) {
              objetoidentificado2 = recog.getTitle().toString();
            }
            // FIM DO ARRAY DE IMAGENS IDENTIFICADAS

            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            LOGGER.i("Detect: %s", results);
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            if (resultsView == null) {
              resultsView = (ResultsView) findViewById(R.id.results);
              //CHAMA O PROCESSO AGOVAI
              /*
              objetoidentificado = recog.getTitle().toString();

                  for (final Recognition recog : results) {
                  //canvas.drawText(recog.getTitle() + ": " + recog.getConfidence(), x, y, fgPaint);
                  objetoidentificado = recog.getTitle().toString();
                      if(objetoidentificado.equals("screen")||objetoidentificado.equals("notebook")||objetoidentificado.equals("laptop")||objetoidentificado.equals("monitor")||objetoidentificado.equals("desktop computer")||objetoidentificado.equals("mouse")||objetoidentificado.equals("computer keyboard")||objetoidentificado.equals("web site")){
                        canvas.drawText("Alerta de vazamento de dados.", x, y, fgPaint);
                        }else{
                        //canvas.drawText("De boa nada, tem que escrever o texto inteiro.", x, y, fgPaint);
                        canvas.drawText(objetoidentificado, x, y, fgPaint);
                      }
                  y += fgPaint.getTextSize() * 1.5f;
                }

              objetoidentificado2 = getTitle().toString();
              if(objetoidentificado2.equals("screen")||objetoidentificado2.equals("notebook")||objetoidentificado2.equals("laptop")||objetoidentificado2.equals("monitor")||objetoidentificado2.equals("desktop computer")||objetoidentificado2.equals("mouse")||objetoidentificado2.equals("computer keyboard")||objetoidentificado2.equals("web site")){
                agoravai();
              }

               */

              //FIM DA CHAMADA PARA O AGORAVAI()




            }
            resultsView.setResults(results);
            requestRender();
            readyForNextImage();
          }
        });
  }

  @Override
  public void onSetDebug(boolean debug) {
    classifier.enableStatLogging(debug);
  }

  private void renderDebug(final Canvas canvas) {
    if (!isDebug()) {
      return;
    }
    final Bitmap copy = cropCopyBitmap;
    if (copy != null) {
      final Matrix matrix = new Matrix();
      final float scaleFactor = 2;
      matrix.postScale(scaleFactor, scaleFactor);
      matrix.postTranslate(
          canvas.getWidth() - copy.getWidth() * scaleFactor,
          canvas.getHeight() - copy.getHeight() * scaleFactor);
      canvas.drawBitmap(copy, matrix, new Paint());

      final Vector<String> lines = new Vector<String>();
      if (classifier != null) {
        // -------------------------------- INICIO
        //String statString = classifier.getStatString(); //original e funcionando
        //String statString = "Pedro"; //Funcionou
        String statString = objetoidentificado2; // Funcionou perfeitamente mostrando o objeto identificado

        if(objetoidentificado2.equals("screen")||objetoidentificado2.equals("notebook")||objetoidentificado2.equals("laptop")||objetoidentificado2.equals("monitor")||objetoidentificado2.equals("desktop computer")||objetoidentificado2.equals("mouse")||objetoidentificado2.equals("computer keyboard")||objetoidentificado2.equals("web site")){

          agoravai();

        }



        // -------------------------------- FIM

        String[] statLines = statString.split("\n");
        for (String line : statLines) {
          lines.add(line);
        }
      }

      lines.add("Frame: " + previewWidth + "x" + previewHeight);
      lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
      lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
      lines.add("Rotation: " + sensorOrientation);
      lines.add("Inference time: " + lastProcessingTimeMs + "ms");

      borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
    }
  }
}
