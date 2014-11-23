package edu.upc.eetac.dsa.mleiva.beeter.api;

/**
 * Created by Administrador on 17/11/2014.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class BeeterAPI {
    private final static String TAG = BeeterAPI.class.getName();
    private static BeeterAPI instance = null;
    private URL url;

    private BeeterRootAPI rootAPI = null;

    private BeeterAPI(Context context) throws IOException, AppException {
        super();

        AssetManager assetManager = context.getAssets();
        Properties config = new Properties();
        config.load(assetManager.open("config.properties"));
        String urlHome = config.getProperty("beeter.home");
        url = new URL(urlHome);

        Log.d("LINKS", url.toString());
        getRootAPI();//llama a la root de la api, modelo que guarda la respuesta a la raiz del servicio
    }

    public final static BeeterAPI getInstance(Context context) throws AppException {
        if (instance == null)
            try {
                instance = new BeeterAPI(context);
            } catch (IOException e) {
                throw new AppException(
                        "Can't load configuration file");
            }
        return instance;
    }

    private void getRootAPI() throws AppException {
        Log.d(TAG, "getRootAPI()");
        rootAPI = new BeeterRootAPI();
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);//vamos a leer
            urlConnection.connect();//Hace la peticion
        } catch (IOException e) {
            throw new AppException(
                    "Can't connect to Beeter API Web Service");
        }

        BufferedReader reader; //leemos la respuesta con el reader
        try {
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }//se guarda con el stringBuilder

            JSONObject jsonObject = new JSONObject(sb.toString());//se procesa el json de respuesta
            JSONArray jsonLinks = jsonObject.getJSONArray("links");//creamos un json a partir de la respuesta
            parseLinks(jsonLinks, rootAPI.getLinks());//del atributo links damelo
        } catch (IOException e) {
            throw new AppException(
                    "Can't get response from Beeter API Web Service");
        } catch (JSONException e) {
            throw new AppException("Error parsing Beeter Root API");
        }

    }

    public StingCollection getStings() throws AppException {
        Log.d(TAG, "getStings()");
        StingCollection stings = new StingCollection();

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(rootAPI.getLinks()
                    .get("stings").getTarget()).openConnection();//dame el target del atributo stings y abrimos conexión
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.connect();
        } catch (IOException e) {
            throw new AppException(
                    "Can't connect to Beeter API Web Service");
        }

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonLinks = jsonObject.getJSONArray("links");
            parseLinks(jsonLinks, stings.getLinks());

            stings.setNewestTimestamp(jsonObject.getLong("newestTimestamp"));
            stings.setOldestTimestamp(jsonObject.getLong("oldestTimestamp"));
            JSONArray jsonStings = jsonObject.getJSONArray("stings");
            for (int i = 0; i < jsonStings.length(); i++) {
                Sting sting = new Sting();
                JSONObject jsonSting = jsonStings.getJSONObject(i);
                sting.setAuthor(jsonSting.getString("author"));
                sting.setStingid(jsonSting.getInt("stingid"));
                sting.setLastModified(jsonSting.getLong("lastModified"));
                sting.setCreationTimestamp(jsonSting.getLong("creationTimestamp"));
                sting.setSubject(jsonSting.getString("subject"));
                sting.setUsername(jsonSting.getString("username"));
                jsonLinks = jsonSting.getJSONArray("links");
                parseLinks(jsonLinks, sting.getLinks());
                stings.getStings().add(sting);
            }
        } catch (IOException e) {
            throw new AppException(
                    "Can't get response from Beeter API Web Service");
        } catch (JSONException e) {
            throw new AppException("Error parsing Beeter Root API");
        }

        return stings;
    }

    private void parseLinks(JSONArray jsonLinks, Map<String, Link> map)//atributo links de tipo map
            throws AppException, JSONException {
        for (int i = 0; i < jsonLinks.length(); i++) {//te dice que hay 2 dentro del array
            Link link = null;
            try {
                link = SimpleLinkHeaderParser
                        .parseLink(jsonLinks.getString(i));//lo parseamos y devuelve un enlace
            } catch (Exception e) {
                throw new AppException(e.getMessage());
            }
            String rel = link.getParameters().get("rel");
            String rels[] = rel.split("\\s");
            for (String s : rels)
                map.put(s, link);
        }
    }



    private Map<String, Sting> stingsCache = new HashMap<String, Sting>();

    public Sting getSting(String urlSting) throws AppException {
        Sting sting = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlSting);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);

            sting = stingsCache.get(urlSting);
            String eTag = (sting == null) ? null : sting.getETag();
            if (eTag != null)
                urlConnection.setRequestProperty("If-None-Match", eTag);
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "CACHE");
                return stingsCache.get(urlSting);
            }
            Log.d(TAG, "NOT IN CACHE");
            sting = new Sting();
            eTag = urlConnection.getHeaderField("ETag");
            sting.setETag(eTag);


            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonSting = new JSONObject(sb.toString());
            sting.setAuthor(jsonSting.getString("author"));
            sting.setStingid(jsonSting.getInt("stingid"));
            sting.setLastModified(jsonSting.getLong("lastModified"));
            sting.setCreationTimestamp(jsonSting.getLong("creationTimestamp"));
            sting.setSubject(jsonSting.getString("subject"));
            sting.setContent(jsonSting.getString("content"));
            sting.setUsername(jsonSting.getString("username"));
            JSONArray jsonLinks = jsonSting.getJSONArray("links");
            parseLinks(jsonLinks, sting.getLinks());
            stingsCache.put(urlSting, sting);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new AppException("Bad sting url");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new AppException("Exception when getting the sting");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new AppException("Exception parsing response");
        }

        return sting;
    }

}