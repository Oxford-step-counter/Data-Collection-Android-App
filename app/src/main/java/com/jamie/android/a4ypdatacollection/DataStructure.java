package com.jamie.android.a4ypdatacollection;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Jamie Brynes on 7/28/2016.
 */
public class DataStructure implements Parcelable{

    private ArrayList<Long> time;
    private ArrayList<Float> x_acc;
    private ArrayList<Float> y_acc;
    private ArrayList<Float> z_acc;

    public int describeContents()
    {
        return 0;
    }

    public int getLength()
    {
        return time.size();
    }

    public String getString()
    {
        String s = "";

        for (int i = 0; i < this.getLength(); i++)
        {
            s = s + time.get(i).toString() + ',' + x_acc.get(i).toString() + ',' + y_acc.get(i).toString() + ',' + z_acc.get(i).toString() + '\n';
        }

        return s;
    }


    public static final Parcelable.Creator<DataStructure> CREATOR = new Parcelable.Creator<DataStructure>()
    {
        public DataStructure createFromParcel(Parcel in)
        {
            return new DataStructure(in);
        }

        public DataStructure[] newArray(int size)
        {
            return new DataStructure[size];
        }

    };

    private DataStructure(Parcel in)
    {
        x_acc = (ArrayList<Float>) in.readSerializable();
        y_acc = (ArrayList<Float>) in.readSerializable();
        z_acc = (ArrayList<Float>) in.readSerializable();
        time = (ArrayList<Long>) in.readSerializable();
    }

    public void writeToParcel(Parcel out, int flags)
    {
        out.writeSerializable(x_acc);
        out.writeSerializable(y_acc);
        out.writeSerializable(z_acc);
        out.writeSerializable(time);
    }

    public DataStructure()
    {
        x_acc =  new ArrayList<Float>();
        y_acc = new ArrayList<Float>();
        z_acc = new ArrayList<Float>();
        time = new ArrayList<Long>();
    }

    public void addDataPoint(long t, float x, float y, float z)
    {
        x_acc.add(x);
        y_acc.add(y);
        z_acc.add(z);
        time.add(t);
    }

    public DataStructure(ArrayList<Float> x, ArrayList<Float> y, ArrayList<Float> z, ArrayList<Long> t)
    {
        x_acc = x;
        y_acc = y;
        z_acc = z;
        time = t;
    }


}
