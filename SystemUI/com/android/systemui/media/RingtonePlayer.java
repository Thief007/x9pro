package com.android.systemui.media;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioAttributes;
import android.media.IAudioService;
import android.media.IRingtonePlayer;
import android.media.IRingtonePlayer.Stub;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.SystemUI;
import com.android.systemui.assis.app.MAIN.EVENT;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class RingtonePlayer extends SystemUI {
    private final NotificationPlayer mAsyncPlayer = new NotificationPlayer("RingtonePlayer");
    private IAudioService mAudioService;
    private IRingtonePlayer mCallback = new Stub() {
        public void play(IBinder token, Uri uri, AudioAttributes aa, float volume, boolean looping) throws RemoteException {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(token);
                if (client == null) {
                    client = new Client(token, uri, Binder.getCallingUserHandle(), aa);
                    token.linkToDeath(client, 0);
                    RingtonePlayer.this.mClients.put(token, client);
                }
            }
            client.mRingtone.setLooping(looping);
            client.mRingtone.setVolume(volume);
            client.mRingtone.play();
        }

        public void stop(IBinder token) {
            synchronized (RingtonePlayer.this.mClients) {
                Client client = (Client) RingtonePlayer.this.mClients.remove(token);
            }
            if (client != null) {
                client.mToken.unlinkToDeath(client, 0);
                client.mRingtone.stop();
            }
        }

        public boolean isPlaying(IBinder token) {
            synchronized (RingtonePlayer.this.mClients) {
                Client client = (Client) RingtonePlayer.this.mClients.get(token);
            }
            if (client != null) {
                return client.mRingtone.isPlaying();
            }
            return false;
        }

        public void setPlaybackProperties(IBinder token, float volume, boolean looping) {
            synchronized (RingtonePlayer.this.mClients) {
                Client client = (Client) RingtonePlayer.this.mClients.get(token);
            }
            if (client != null) {
                client.mRingtone.setVolume(volume);
                client.mRingtone.setLooping(looping);
            }
        }

        public void playAsync(Uri uri, UserHandle user, boolean looping, AudioAttributes aa) {
            if (Binder.getCallingUid() != EVENT.DYNAMIC_PACK_EVENT_BASE) {
                throw new SecurityException("Async playback only available from system UID.");
            }
            if (UserHandle.ALL.equals(user)) {
                user = UserHandle.OWNER;
            }
            RingtonePlayer.this.mAsyncPlayer.play(RingtonePlayer.this.getContextForUser(user), uri, looping, aa);
        }

        public void stopAsync() {
            if (Binder.getCallingUid() != EVENT.DYNAMIC_PACK_EVENT_BASE) {
                throw new SecurityException("Async playback only available from system UID.");
            }
            RingtonePlayer.this.mAsyncPlayer.stop();
        }

        public String getTitle(Uri uri) {
            return Ringtone.getTitle(RingtonePlayer.this.getContextForUser(Binder.getCallingUserHandle()), uri, false, false);
        }
    };
    private final HashMap<IBinder, Client> mClients = new HashMap();

    private class Client implements DeathRecipient {
        private final Ringtone mRingtone;
        private final IBinder mToken;

        public Client(IBinder token, Uri uri, UserHandle user, AudioAttributes aa) {
            this.mToken = token;
            this.mRingtone = new Ringtone(RingtonePlayer.this.getContextForUser(user), false);
            this.mRingtone.setAudioAttributes(aa);
            this.mRingtone.setUri(uri);
        }

        public void binderDied() {
            synchronized (RingtonePlayer.this.mClients) {
                RingtonePlayer.this.mClients.remove(this.mToken);
            }
            this.mRingtone.stop();
        }
    }

    public void start() {
        this.mAsyncPlayer.setUsesWakeLock(this.mContext);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        try {
            this.mAudioService.setRingtonePlayer(this.mCallback);
        } catch (RemoteException e) {
            Log.e("RingtonePlayer", "Problem registering RingtonePlayer: " + e);
        }
    }

    private Context getContextForUser(UserHandle user) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Clients:");
        synchronized (this.mClients) {
            for (Client client : this.mClients.values()) {
                pw.print("  mToken=");
                pw.print(client.mToken);
                pw.print(" mUri=");
                pw.println(client.mRingtone.getUri());
            }
        }
    }
}
