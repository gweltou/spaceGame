package com.gwel.spacegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;


public class DialogManager implements Disposable {
    private final String[] greetingPos, greetingNeg;
    private final String[] pronounPos, pronounNeu, pronounNeg;
    private final String[] activityPos, activityNeu, activityNeg;
    private final String[] questionPos, questionNeu, questionNeg;
    private final String[][] phrase;

    public DialogManager() {
        FileHandle jsonFile = Gdx.files.internal("dialogs.json");
        //String json = jsonFile.readString();
        JsonValue root = new JsonReader().parse(jsonFile);
        greetingPos = root.get("greeting_pos").asStringArray();
        greetingNeg = root.get("greeting_neg").asStringArray();
        pronounPos = root.get("pronoun_pos").asStringArray();
        pronounNeu = root.get("pronoun_neu").asStringArray();
        pronounNeg = root.get("pronoun_neg").asStringArray();
        activityPos = root.get("activity_pos").asStringArray();
        activityNeu = root.get("activity_neu").asStringArray();
        activityNeg = root.get("activity_neg").asStringArray();
        questionPos = root.get("question_pos").asStringArray();
        questionNeu = root.get("question_neu").asStringArray();
        questionNeg = root.get("question_neg").asStringArray();

        JsonValue phraseRoot = root.get("phrase");
        phrase = new String[phraseRoot.size][];
        for (int i=0; i<phraseRoot.size; i++) {
            phrase[i] = phraseRoot.get(i).asStringArray();
        }

        System.out.println("Dialog Manager created");
    }

    public String getPosPhrase() {
        int r = MathUtils.random(phrase.length-1);
        String text = new String();
        for (String element : phrase[r]) {
            if (element.contentEquals("greeting")) {
                text += greetingPos[MathUtils.random(greetingPos.length-1)];
            } else if (element.contentEquals("pronoun")) {
                text += pronounPos[MathUtils.random(pronounPos.length-1)];
            } else if (element.contentEquals("question")) {
                text += questionPos[MathUtils.random(questionPos.length-1)];
            } else if (element.contentEquals("activity")) {
                text += activityPos[MathUtils.random(activityPos.length-1)];
            } else {
                text += element;
            }
        }
        System.out.println(text);
        return capitalize(text);
    }

    public static String capitalize(String str) {
        if(str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void dispose() {

    }
}
