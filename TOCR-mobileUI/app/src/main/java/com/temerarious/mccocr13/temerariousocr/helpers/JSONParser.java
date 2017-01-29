package com.temerarious.mccocr13.temerariousocr.helpers;

/**
 * Created by ivan on 29.11.16.
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
public class JSONParser {
    public static ArrayList<String> favAthletes = new ArrayList<>();
    public static ArrayList<String> favTeams = new ArrayList<>();


    public static ArrayList<String> getFavAthletes(JSONObject obj) {
        favAthletes.clear();
        try {

            JSONArray arr = obj.getJSONArray("favorite_athletes");
            String s;
            for (int i = 0; i < arr.length(); i++) {
                obj = arr.getJSONObject(i);
                s = obj.getString("name");
                favAthletes.add(s);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return favAthletes;
    }

    public static ArrayList<String> getFavTeams(JSONObject obj) {
        favTeams.clear();
        try {

            JSONArray arr = obj.getJSONArray("favorite_teams");
            String s;
            for (int i = 0; i < arr.length(); i++) {
                obj = arr.getJSONObject(i);
                s = obj.getString("name");
                favTeams.add(s);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return favTeams;
    }

    public static String getName(JSONObject obj) {
        String s1 = "";
        try {

            s1 = obj.getString("name");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return s1;
    }


    public static String getId(JSONObject obj) {
        String s1 = "";
        try {

            s1 = obj.getString("id");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return s1;
    }
}