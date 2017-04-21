package com.android.systemui.statusbar.policy;

import android.content.Context;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import java.util.BitSet;

public class EthernetSignalController extends SignalController<State, IconGroup> {
    public EthernetSignalController(Context context, CallbackHandler callbackHandler, NetworkControllerImpl networkController) {
        super("EthernetSignalController", context, 3, callbackHandler, networkController);
        State state = this.mCurrentState;
        IconGroup iconGroup = new IconGroup("Ethernet Icons", EthernetIcons.ETHERNET_ICONS, null, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES, 0, 0, 0, 0, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES[0]);
        this.mLastState.iconGroup = iconGroup;
        state.iconGroup = iconGroup;
    }

    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        this.mCurrentState.connected = connectedTransports.get(this.mTransportType);
        super.updateConnectivity(connectedTransports, validatedTransports);
    }

    public void notifyListeners() {
        this.mCallbackHandler.setEthernetIndicators(new IconState(this.mCurrentState.connected, getCurrentIconId(), getStringIfExists(getContentDescription())));
    }

    public State cleanState() {
        return new State();
    }
}
