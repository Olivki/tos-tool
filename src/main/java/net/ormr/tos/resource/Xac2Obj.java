package net.ormr.tos.resource;/*
 * Tree of Savior
 * .xac -> .obj
 * 2015-01-19
 */

import java.io.*;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Scanner;

public class Xac2Obj {

    private static Scanner scanner;
    private static DataInputStream in = null;

    private static float readFloat(DataInputStream d) throws IOException {
        return Float.intBitsToFloat(readInt(d));
    }

    private static int readInt(DataInputStream d) throws IOException {
        byte[] w = new byte[4];
        d.readFully(w, 0, 4);
        return (w[3]) << 24 | (w[2] & 0xFF) << 16 | (w[1] & 0xFF) << 8 | (w[0] & 0xFF);
    }

    private static String readString(DataInputStream d, int length) throws IOException {
        byte[] bytes = new byte[length];
        d.readFully(bytes, 0, length);
        return new String(bytes);
    }

    public static void main(String[] args) {

        int numObjects = 0;
        int objectIndex = 0;
        DecimalFormat df;

        Locale.setDefault(new Locale("en", "US"));
        df = new DecimalFormat("0.00000000");

        try {

            String filename;
            scanner = new Scanner(System.in);
            if (args.length != 1) {
                System.out.print("\n>");
                filename = scanner.next();
            } else {
                filename = args[0];
            }

            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("\nFile " + filename + " doesn't exist!");
                System.exit(1);
            }

            filename = file.getAbsolutePath();

            in = new DataInputStream(new FileInputStream(filename));
            String magicWord = readString(in, 4);

            if (!magicWord.equals("XAC ")) {
                System.out.println("Not a valid XAC file!");
                exit();
            }

            in.skipBytes(4);

            boolean eof = false;
            while (!eof) {
                // read chunk header
                int id = readInt(in);
                int len = readInt(in);
                int ver = readInt(in);

                switch (id) {
                    case 3: /*fallthrough*/
                    case 7:
                    case 0xB:
                        in.skipBytes(len);
                        break;
                    case 0xD:
                        in.skipBytes(8);
                        numObjects = readInt(in);
                        break;
                    case 5:
                        // material chunk
                        // TODO: parse this chunk to obtain textures
                        in.skipBytes(len);
                        break;
                    case 1:
                        // object chunk
                        in.skipBytes(8);
                        int numVertices = readInt(in);
                        int numFaces = readInt(in);
                        in.skipBytes(0x10);
                        int numVertexBlocks = readInt(in);
                        in.skipBytes(4);

                        in.skipBytes(numVertices * 4);

                        boolean gotGeometry = false;
                        boolean gotNormals = false;
                        boolean gotUv = false;

                        float[] geometry = new float[numVertices * 3];
                        float[] normals = new float[numVertices * 3];
                        float[] uv = new float[numVertices * 2];
                        int[] faces = new int[numFaces];

                        for (int i = 0; i < numVertexBlocks; i++) {
                            int blockId = readInt(in);
                            int blockSize = readInt(in);
                            in.skipBytes(4);

                            if (blockId == 0 && !gotGeometry) {
                                for (int j = 0; j < numVertices * 3; j++) {
                                    geometry[j] = readFloat(in);
                                }
                                gotGeometry = true;
                            } else if (blockId == 1 && !gotNormals) {
                                for (int j = 0; j < numVertices * 3; j++) {
                                    normals[j] = readFloat(in);
                                }
                                gotNormals = true;
                            } else if (blockId == 3 && !gotUv) {
                                for (int j = 0; j < numVertices * 2; j++) {
                                    uv[j] = readFloat(in);
                                }
                                gotUv = true;
                            } else {
                                in.skipBytes(numVertices * blockSize);
                            }
                        }

                        int numFacesCheck = readInt(in);
                        int numVerticesCheck = readInt(in);
                        in.skipBytes(8);

                        if (numFacesCheck != numFaces || numVerticesCheck != numVertices) {
                            System.out.println("Face/Vertex check failed!");
                        } else {
                            for (int i = 0; i < numFaces; i++) {
                                faces[i] = readInt(in);
                            }

                            // create OBJ file
                            FileWriter out = new FileWriter(new File(filename + "." + objectIndex + ".obj"));

                            for (int i = 0; i < numVertices; i++) {
                                out.write(String.format("v %s %s %s%n", df.format(geometry[i * 3]), df.format(geometry[i * 3 + 1]), df.format(geometry[i * 3 + 2])));
                            }
                            out.write(String.format("%n"));

                            for (int i = 0; i < numVertices; i++) {
                                out.write(String.format("vn %s %s %s%n", df.format(normals[i * 3]), df.format(normals[i * 3 + 1]), df.format(normals[i * 3 + 2])));
                            }
                            out.write(String.format("%n"));

                            for (int i = 0; i < numVertices; i++) {
                                out.write(String.format("vt %s %s%n", df.format(uv[i * 2]), df.format(1 - uv[i * 2 + 1])));
                            }
                            out.write(String.format("%n"));

                            for (int i = 0; i < numFaces / 3; i++) {
                                int a = faces[i * 3] + 1;
                                int b = faces[i * 3 + 1] + 1;
                                int c = faces[i * 3 + 2] + 1;
                                out.write(String.format("f %d/%d/%d %d/%d/%d %d/%d/%d%n", a, a, a, b, b, b, c, c, c));
                            }
                            out.write(String.format("%n"));

                            out.close();

                            objectIndex++;

                            if (objectIndex == numObjects) {
                                eof = true;
                            }
                        }

                        break;
                    default:
                        System.out.printf("Unknown identifier: %d (length: %d, version: %d)%n", id, len, ver);
                        in.skipBytes(len);
                        break;
                }

            }
            exit();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void exit() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        System.exit(0);
    }

}
