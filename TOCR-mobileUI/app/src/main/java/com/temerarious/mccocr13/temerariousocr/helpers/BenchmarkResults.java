package com.temerarious.mccocr13.temerariousocr.helpers;

/**
 * Created by fabiano.brito on 08/12/2016.
 */

public class BenchmarkResults {
    double[] localElapsedTime;
    double[] remoteElapsedTime;
    double[] dataExchanged;
    int numberOfFiles;
    double localTotal = 0;
    double remoteTotal = 0;
    double dataTotal = 0;
    double localAverage = 0;
    double remoteAverage = 0;
    double dataAverage = 0;

    public void setNumberOfFiles(int files) {
        localElapsedTime = new double[files];
        remoteElapsedTime = new double[files];
        dataExchanged = new double[files];
        numberOfFiles = files;
    }

    public int getNumberOfFiles() {
        return numberOfFiles;
    }

    public void setLocalElapsedTime(int position, double time) {
        localElapsedTime[position] = time;
    }

    public void setRemoteElapsedTime(int position, double time) {
        remoteElapsedTime[position] = time;
    }

    public void setDataExchanged(int position, double data) {
        dataExchanged[position] = data;
    }

    public double getLocalElapsedTime(int position) {
        return localElapsedTime[position];
    }

    public double getRemoteElapsedTime(int position) {
        return remoteElapsedTime[position];
    }

    public double getDataExchanged(int position) {
        return dataExchanged[position];
    }

    public double getLocalTotal() {
        for (int i = 0; i < localElapsedTime.length; i++) {
            localTotal += localElapsedTime[i];
        }
        return localTotal;
    }

    public double getRemoteTotal() {
        for (int i = 0; i < remoteElapsedTime.length; i++) {
            remoteTotal += remoteElapsedTime[i];
        }
        return remoteTotal;
    }

    public double getDataExchangedTotal() {
        for (int i = 0; i < dataExchanged.length; i++) {
            dataTotal += dataExchanged[i];
        }
        return dataTotal;
    }

    public double getLocalAverage() {
        localAverage = localTotal / numberOfFiles;
        return localAverage;
    }

    public double getRemoteAverage() {
        remoteAverage = remoteTotal / numberOfFiles;
        return remoteAverage;
    }

    public double getDataExchangedAverage() {
        dataAverage = dataTotal / numberOfFiles;
        return dataAverage;
    }

    public double getLocalDeviation() {
        double deviation = 0;
        for (int i = 0; i < localElapsedTime.length; i++) {
            deviation += Math.pow((localElapsedTime[i] - localAverage), 2);
        }
        deviation /= localAverage;
        return Math.sqrt(deviation);
    }

    public double getRemoteDeviation() {
        double deviation = 0;
        for (int i = 0; i < remoteElapsedTime.length; i++) {
            deviation += Math.pow((remoteElapsedTime[i] - remoteAverage), 2);
        }
        deviation /= remoteAverage;
        return Math.sqrt(deviation);
    }

    public double getDataExchangedDeviation() {
        double deviation = 0;
        for (int i = 0; i < dataExchanged.length; i++) {
            deviation += Math.pow((dataExchanged[i] - dataAverage), 2);
        }
        deviation /= dataAverage;
        return Math.sqrt(deviation);
    }

    public int getLocalMaxIndex() {
        int index = 0;
        for (int i = 1; i < numberOfFiles; i++) {
            if (localElapsedTime[i] > localElapsedTime[i - 1]) {
                index = i;
            }
        }
        return index + 1;
    }

    public int getLocalMinIndex() {
        int index = 0;
        for (int i = 1; i < numberOfFiles; i++) {
            if (localElapsedTime[i] < localElapsedTime[i - 1]) {
                index = i;
            }
        }
        return index + 1;
    }

    public int getRemoteMaxIndex() {
        int index = 0;
        for (int i = 1; i < numberOfFiles; i++) {
            if (remoteElapsedTime[i] > remoteElapsedTime[i - 1]) {
                index = i;
            }
        }
        return index + 1;
    }

    public int getRemoteMinIndex() {
        int index = 0;
        for (int i = 1; i < numberOfFiles; i++) {
            if (remoteElapsedTime[i] < remoteElapsedTime[i - 1]) {
                index = i;
            }
        }
        return index + 1;
    }

    public int getDataExchangedMaxIndex() {
        int index = 0;
        for (int i = 1; i < numberOfFiles; i++) {
            if (dataExchanged[i] > dataExchanged[i - 1]) {
                index = i;
            }
        }
        return index + 1;
    }

    public int getDataExchangedMinIndex() {
        int index = 0;
        for (int i = 1; i < numberOfFiles; i++) {
            if (dataExchanged[i] < dataExchanged[i - 1]) {
                index = i;
            }
        }
        return index + 1;
    }

}
