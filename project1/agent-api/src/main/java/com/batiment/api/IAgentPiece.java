package com.batiment.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAgentPiece extends Remote {
    double getTemperature() throws RemoteException;
    int getLuminosite() throws RemoteException;
    boolean isOccupee() throws RemoteException;
    void setConsigneTemperature(double consigne) throws RemoteException;
    String getEtatJson() throws RemoteException;
}
