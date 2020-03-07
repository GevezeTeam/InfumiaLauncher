/**
 *    Copyright 2019-2020 Infumia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.infumia.launcher.download;

import com.infumia.launcher.InfumiaLauncher;
import com.infumia.launcher.objects.Callback;
import com.sun.javafx.PlatformUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kamranzafar.jddl.DirectDownloader;
import org.kamranzafar.jddl.DownloadListener;
import org.kamranzafar.jddl.DownloadTask;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CustomDownloader {
    private Callback errorCallback;
    private String version;
    private JSONObject versionObject;
    private Storage storage;

    public CustomDownloader(Storage storage, Callback errorCallback) {
        this.errorCallback = errorCallback;
        this.storage = storage;
        this.version = storage.isIllegalVersion() ? storage.getAssetVersion() : storage.getVersion();
        this.versionObject = storage.getVersionObject();

        sorted = versionCheck();
    }

    int totalFile = 0;

    int currentfilelib = 0;
    int currentfilenativelib = 0;

    File librariesDir = new File(getMineCraftLocation() + "/libraries/");


    private boolean libsDownloaded = false;
    private boolean nativesDownloaded = false;

    JSONArray objects = null;
    List sorted;

    public void run() {
        if (objects == null) {
            totalFile = storage.getLocal().version_url_list.size() + storage.getLocal().version_url_list_natives.size();
            storage.setTotalLibraries(totalFile);
        }

        if (libsDownloaded && nativesDownloaded) {
            System.out.print("\r");
            InfumiaLauncher.logger.info("Oyun baslatiliyor");
            InfumiaLauncher.step++;
            new Minecraft(storage).launchMinecraft();
            return;
        }


        String dirs = "";
        String fullName = storage.getLocal().version_path_list.get(currentfilelib).toString();
        String[] fullNameSplitted = storage.getLocal().version_path_list.get(currentfilelib).toString().split("/");
        String name = fullName.split("/")[fullNameSplitted.length - 1];
        for (String str : fullNameSplitted) {
            if (!str.contains(".jar")) dirs += str + "/";
        }

        File libDir = new File(storage.getUtils().getMineCraftLibrariesLocation(storage.getOperationgSystem()) + "/" + dirs);
        libDir.mkdirs();

        libDir = new File(storage.getUtils().getMineCraftLibrariesLocation(storage.getOperationgSystem()) + "/" + dirs, name);

        if (currentfilelib == storage.getLocal().version_url_list.size()) {
            System.out.print("\r");
            libsDownloaded = true;
            InfumiaLauncher.logger.info("Natives dosyasi aktiflestirildi isletim sisteminiz kontrol ediliyor");
            runNatives();
            return;
        }
        try {

            DirectDownloader dd = new DirectDownloader();
            if (libDir.exists() && !libDir.isDirectory()) {
                if (storage.getLocal().version_hash_list.get(currentfilelib).equals(calcSHA1(libDir))) {
                    System.out.print("\r");
                    System.out.print("Dosya zaten var diger dosyaya geciliyor. " + (currentfilelib + currentfilenativelib) + "/" + storage.getLocal().version_url_list.size());
                    currentfilelib++;
                    storage.setDownloadedLib(currentfilelib);
                    run();
                    return;
                }
            }

            dd.download(new DownloadTask(new URL(storage.getLocal().version_url_list.get(currentfilelib).toString()), new FileOutputStream(new File(librariesDir + "/" + dirs, name)), new DownloadListener() {

                String fname;
                double fileSize = 0;

                @Override
                public void onStart(String fname, int fsize) {
                    this.fname = fname;
                    fileSize = fsize;
                }

                @Override
                public void onUpdate(int bytes, int totalDownloaded) {
                    double t1 = totalDownloaded + 0.0d;
                    double t2 = fileSize + 0.0d;
                    double downloadpercent = (t1 / t2) * 100.0d;
                    System.out.print("\r" + ">Indiriliyor " + fname + " %" + new DecimalFormat("##.#").format(downloadpercent).replace(",", "."));
                }

                @Override
                public void onComplete() {
                    try {
                        currentfilelib++;
                        storage.setDownloadedLib(currentfilelib);
                        run();
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorCallback.response(e.toString());
                    }
                }

                @Override
                public void onCancel() {

                }
            }));
            Thread t = new Thread(dd);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
            errorCallback.response(e.toString());
        }
    }

    boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        }catch (NumberFormatException e) {
            return false;
        }
    }

    List versionCheck() {
        List list = new ArrayList<String>();
        list.addAll(storage.getLocal().version_path_list_natives);

        List removeList = new ArrayList<String>();

        Collections.sort(list, (a, b)-> {
            if (a == null || b == null) return 0;
            File aFile = new File((String) a);
            File bFile = new File((String) b);
            String aname = aFile.getName();
            if (aname.isEmpty()) return 0;
            String aremoved = aname.substring(0, aname.lastIndexOf('.'));
            String bname = bFile.getName();
            if (bname.isEmpty()) return 0;
            String bremoved = bname.substring(0, bname.lastIndexOf('.'));
            for (String str : aremoved.split("-")) {
                if (isInteger(str)) {
                    if (Integer.parseInt(str) > 1000) {
                        aremoved = aremoved.replaceAll("-" + str, "");
                    }
                }
            }
            for (String str : bremoved.split("-")) {
                if (isInteger(str)) {
                    if (Integer.parseInt(str) > 1000) {
                        bremoved = bremoved.replaceAll("-" + str, "");
                    }
                }
            }
            int versiona = Integer.parseInt(aremoved.replaceAll("[\\D]", ""));
            int versionB = Integer.parseInt(bremoved.replaceAll("[\\D]", ""));
            // lwjgl-tinyfd-3.2.1-natives-windows lwjgl-tinyfd-3.2.2-natives-windows
            String formattedvera = aremoved.replaceAll(storage.getLocal().getNatives_OS(getOS()), "").replaceAll("[A-Za-z]?", "").replaceAll("-", "");
            String formattedverb = bremoved.replaceAll(storage.getLocal().getNatives_OS(getOS()), "").replaceAll("[A-Za-z]?", "").replaceAll("-", "");
            if (!aname.replaceAll(storage.getLocal().getNatives_OS(getOS()), "").replaceAll(formattedvera, "").equals(
                    bname.replaceAll(storage.getLocal().getNatives_OS(getOS()), "").replaceAll(formattedverb, ""))) return 0;
            if (versiona == versionB) return 0;
            if (versiona > versionB){
                if (!removeList.contains(b)) {
                    removeList.add(b);
                }
                return 1;
            }
            if (versiona < versionB) {
                if (!removeList.contains(a)) removeList.add(a);
                return -1;
            }
            return 0;
        });

        List sortedList = list;
        sortedList.removeAll(removeList);

        return sortedList;
    }

    boolean containsListString(List list, String obj) {
        for (String elmnt : (List<String>) list) {
            if (elmnt.contains(obj)) return true;
        }
        return false;
    }

    public void runNatives() {
        try {
            sorted = versionCheck();
            if (currentfilenativelib == storage.getLocal().version_url_list_natives.size()) {
                System.out.print("\r");
                InfumiaLauncher.logger.info(storage.getOperationgSystem() + " libleri indirildi");
                currentfilelib++;
                storage.setDownloadedLib(currentfilelib);
                nativesDownloaded = true;
                run();
                return;
            }

            File nativeDir = new File(storage.getUtils().getMineCraft_Versions_X_Natives(storage.getOperationgSystem(), storage.getVersion()));
            nativeDir.mkdir();

            String dirs = "";
            String fullName = storage.getLocal().version_path_list_natives.get(currentfilenativelib).toString();
            String[] fullNameSplitted = storage.getLocal().version_path_list_natives.get(currentfilenativelib).toString().split("/");
            String name = fullName.split("/")[fullNameSplitted.length - 1];
            for (String str : fullNameSplitted) {
                if (!str.contains(".jar")) dirs += str + "/";
            }

            File libDir = new File(storage.getUtils().getMineCraftLibrariesLocation(storage.getOperationgSystem()) + "/" + dirs);
            libDir.mkdirs();

            libDir = new File(storage.getUtils().getMineCraftLibrariesLocation(storage.getOperationgSystem()) + "/" + dirs, name);

            DirectDownloader ddnative = new DirectDownloader();
            if (libDir.exists()) {
                if (storage.getLocal().version_hash_list_natives.get(currentfilenativelib).equals(calcSHA1(libDir))) {
                    if (sorted.contains(fullName)) {
                        System.out.print("\r>Dosya çıkartılıyor: " + name);
                        jarExtract(dirs + name, getMineCraft_Versions_X_Natives_Location(version));
                    }
                    System.out.print("\r");
                    currentfilenativelib++;
                    storage.setDownloadedNatives(currentfilenativelib);
                    System.out.print("Diger dosyaya geciliyor. " + currentfilenativelib + "/" + storage.getLocal().version_url_list_natives.size());
                    runNatives();
                    return;
                }
            }

            File downloadedFile = new File(librariesDir + "/" + dirs, name);

            if (downloadedFile.exists()) {
                System.out.print("\r");
                currentfilenativelib++;
                storage.setDownloadedNatives(currentfilenativelib);
                System.out.print("Diger dosyaya geciliyor. " + currentfilenativelib + "/" + storage.getLocal().version_url_list_natives.size());
                runNatives();
                return;
            }

            String finalDirs = dirs;
            ddnative.download(new DownloadTask(new URL(storage.getLocal().version_url_list_natives.get(currentfilenativelib).toString()), new FileOutputStream(downloadedFile), new DownloadListener() {

                String fname;
                double fileSize = 0;

                @Override
                public void onStart(String fname, int fsize) {
                    this.fname = fname;
                    fileSize = fsize;
                    //SLauncher.logger.info("Downloading " + fname + " of size " + fsize + " " + getNativeLibName("windows",currentfilenativelib));
                }

                @Override
                public void onUpdate(int bytes, int totalDownloaded) {
                    double t1 = totalDownloaded + 0.0d;
                    double t2 = fileSize + 0.0d;
                    double downloadpercent = (t1 / t2) * 100.0d;
                    System.out.print("\r" + ">Indiriliyor " + fname + " %" + new DecimalFormat("##.#").format(downloadpercent).replace(",", ".") + " " + currentfilenativelib + "/" + storage.getLocal().version_url_list_natives.size());
                }

                @Override
                public void onComplete() {
                    try {
                        currentfilenativelib++;
                        storage.setDownloadedNatives(currentfilenativelib);
                        if (sorted.contains(fullName)) jarExtract(finalDirs + name, getMineCraft_Versions_X_Natives_Location(version));
                        runNatives();
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorCallback.response(e.toString());
                    }
                }

                @Override
                public void onCancel() {

                }
            }));
            Thread t = new Thread(ddnative);
            t.start();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getOS() {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

        if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
            return ("osx");
        } else if (OS.indexOf("win") >= 0) {
            return ("windows");
        } else if (OS.indexOf("nux") >= 0) {
            return ("linux");
        } else {
            //bring support to other OS.
            //we will assume that the OS is based on linux.
            return ("linux");
        }
    }


    public void jarExtract(String _jarFile, String destDir) {
        try {
            _jarFile = setMineCraft_Versions_X_NativesLocation(_jarFile);
            File dir = new File(destDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File jarFile = new File(_jarFile);

            java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
            java.util.Enumeration enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
                java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());
                if (file.getName().endsWith(".MF")) continue;
                if (file.isDirectory()) { // if its a directory, don't create it
                    continue;
                }
                if (!f.exists()) {
                    java.io.InputStream is = jar.getInputStream(file); // get the input stream
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                    while (is.available() > 0) {  // write contents of 'is' to 'fos'
                        fos.write(is.read());
                    }
                    fos.close();
                    is.close();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMineCraftLibrariesLocation() {
        return (getMineCraftLocation() + "/libraries");
    }

    public String setMineCraft_Versions_X_NativesLocation(String _path) {
        return (getMineCraftLibrariesLocation() + "/" + _path);

    }

    public String getMineCraftVersionsLocation() {
        return (getMineCraftLocation() + "/versions");
    }

    public String getMineCraft_Versions_X_Natives_Location(String VersionNumber) {
        return (getMineCraftVersionsLocation() + "/" + VersionNumber + "/natives");
    }

    public String getMineCraftLocation() {
        if (PlatformUtil.isWindows()) {
            return (System.getenv("APPDATA") + "/.infumia");
        }
        if (PlatformUtil.isLinux()) {
            return (System.getProperty("user.home") + "/.infumia");
        }
        if (PlatformUtil.isMac()) {
            return (System.getProperty("user.home") + "/Library/Application Support/infumia");
        }
        return "N/A";
    }

    private String calcSHA1(File file) throws FileNotFoundException,
            IOException, NoSuchAlgorithmException {

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }

            byte[] digest = sha1.digest();

            return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1,
                    digest));
        }
    }
//        https://libraries.minecraft.net/com/mojang/netty/1.6/netty-1.6.jar.sha1
//        URL url = new URL("https://libraries.minecraft.net/com/mojang/netty/1.6/netty-1.6.jar.sha1");
//        BufferedReader read = new BufferedReader(
//                new InputStreamReader(url.openStream()));
//        String i;
//        while ((i = read.readLine()) != null)
//            System.out.println(i);
//        read.close();

}
