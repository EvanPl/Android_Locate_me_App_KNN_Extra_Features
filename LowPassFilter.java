package com.example.vaggelis.assignment_2;

/**
 * Created by Evan on 20/02/2018.
 */

//Note that the low-pass filter is being used in order to isolate the force of gravity.
//Also, note that when the phone is sitting on the table the accelerometer reads a magnitude of
//9.81m/(s^2). Similarly when the device is in free-fall and thus accelerating towards the ground
//with 9.81m/(s^2), the accelerometer reads a magnitude of 0m/(s^2). Thus, we need to isolate the force
//of gravity using a low-pass filter.
//Moreover, the sensors have high sensitivity and thus the low-pass filter is used to omit/filter out
//the high frequencies in the input signal

public class LowPassFilter {

    private static final float ALPHA=0.98f; //ALPHA is the cut-off/threshold
    private LowPassFilter(){}

    //Note that, the length of the input (which is values) depends on which
    //sensor type is being monitored (accelerometer or magnetic field hardware sensors)
    public static float[] filter (float[] input, float[] output){
        if (output==null) return input;
        for (int i=0; i<input.length;i++){
            output[i]=output[i]*ALPHA+ (1-ALPHA)* input[i];
        }
        return output;
    }

}
