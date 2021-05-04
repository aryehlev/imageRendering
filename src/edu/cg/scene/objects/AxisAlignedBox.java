package edu.cg.scene.objects;

import edu.cg.algebra.*;

import static edu.cg.algebra.Ops.epsilon;


// TODO Implement this class which represents an axis aligned box
public class AxisAlignedBox extends Shape{
    private final static int NDIM=3; // Number of dimensions
    private Point a = null;
    private Point b = null;
    private double[] aAsArray;
    private double[] bAsArray;

    public AxisAlignedBox(Point a, Point b){
        this.a = a;
        this.b = b;
        // We store the points as Arrays - this could be helpful for more elegant implementation.
        aAsArray = a.asArray();
        bAsArray = b.asArray();
        assert (a.x <= b.x && a.y<=b.y && a.z<=b.z);
    }

    @Override
    public String toString() {
        String endl = System.lineSeparator();
        return "AxisAlignedBox:" + endl +
                "a: " + a + endl +
                "b: " + b + endl;
    }

    public AxisAlignedBox initA(Point a){
        this.a = a;
        aAsArray = a.asArray();
        return this;
    }

    public AxisAlignedBox initB(Point b){
        this.b = b;
        bAsArray = b.asArray();
        return this;
    }

    @Override
    public Hit intersect(Ray ray) {
        Vec direction = ray.direction();
        Point source = ray.source();
        if (!checkIfDirectionZeroIsOk(source.x, source.y, source.z, direction)) {
            return null;
        } else {
            double tX0 = (a.x - source.x) / direction.x;
            double tX1 = (b.x - source.x) / direction.x;
            double tY0 = (a.y - source.y) / direction.y;
            double tY1 = (b.y - source.y) / direction.y;
            double tZ0 = (a.z - source.z) / direction.z;
            double tZ1 = (b.z - source.z) / direction.z;


            boolean[] turnAroundNormal = getNormalNegate(tX0, tX1, tY0,tY1, tZ0, tZ1);

            double minTX = getMinX(tX0, tX1, direction.x);
            double minTY = getMinY(tY0, tY1, direction.y);
            double minTZ = getMinZ(tZ0, tZ1, direction.z);

            double maxTX = getMaxX(tX0, tX1, direction.x);
            double maxTY = getMaxY(tY0, tY1, direction.y);
            double maxTZ = getMaxZ(tZ0, tZ1, direction.z);

            double[] tmax = getTMax(maxTX, maxTY, maxTZ);

            double[] tmin = getTMin(minTX, minTY, minTZ);
            Vec normalToSurface;

            if (tmax[0] >= tmin[0] && tmax[0] > epsilon) {
                if (tmin[0] <= epsilon) {
                    tmin = tmax;
                    normalToSurface = getNormalToSurface(tmin[1]);
                }
                else {
                    normalToSurface = getNormalToSurface(tmin[1]).neg();
                }
                if (turnAroundNormal[(int) tmin[1]]) {
                    normalToSurface = normalToSurface.neg();
                }
                return new Hit(tmin[0], normalToSurface);
            } else {
                return null;
            }
        }
    }

    private boolean[] getNormalNegate(double tX0, double tX1, double tY0, double tY1, double tZ0, double tZ1) {
        boolean[] turnAroundNormal = new boolean[3];
        if (tX0 > epsilon && (tX1 <= epsilon || tX1 >= tX0)) {
            turnAroundNormal[0] = false;
        } else {
            turnAroundNormal[0] = true;
        }
        if (tY0 > epsilon && (tY1 <= epsilon || tY1 >= tY0)) {
            turnAroundNormal[1] = false;
        } else {
            turnAroundNormal[1] = true;
        }
        if (tZ0 > epsilon && (tZ1 <= epsilon || tZ1 >= tZ0)) {
            turnAroundNormal[2] = false;
        } else {
            turnAroundNormal[2] = true;
        }

        return turnAroundNormal;
    }


    private Vec getNormalToSurface(double v) {
        if (v == 0) {
            return new Vec(1,0,0);
        }
        else if(v == 1) {
            return new  Vec(0,1,0);
        }
        else{
            return new Vec(0,0,1);
        }
    }

    private double[] getTMax(double maxTX, double maxTY, double maxTZ) {
        double dimension = 0;
        double t = maxTX;
        if (maxTY < t) {
            dimension = 1;
            t = maxTY;
        }
        if (maxTZ < t) {
            dimension = 2;
            t = maxTZ;
        }

        return new double[]{t, dimension};

    }

    private double[] getTMin(double minTX, double minTY, double minTZ) {
        double dimension = 0;
        double t = minTX;
        if (minTY > t) {
            dimension = 1;
            t = minTY;
        }
        if (minTZ > t) {
            dimension = 2;
            t = minTZ;
        }
        return new double[]{t, dimension};

    }

    private double getMinX(double tX0, double tX1, double direction) {
        if (Math.abs(direction) < epsilon) {
            return Double.MIN_VALUE;
        } else {
            return Math.min(tX0, tX1);
        }
    }

    private double getMinY(double tY0, double tY1, double direction) {
        if (Math.abs(direction) < epsilon) {
            return Double.MIN_VALUE;
        } else {
            return Math.min(tY0, tY1);
        }
    }

    private double getMinZ(double tZ0, double tZ1, double direction) {
        if (Math.abs(direction) < epsilon) {
            return Double.MIN_VALUE;
        } else {
            return Math.min(tZ0, tZ1);
        }
    }

    private double getMaxX(double tX0, double tX1, double direction) {
        if (Math.abs(direction) < epsilon) {
            return Double.MAX_VALUE;
        } else {
            return Math.max(tX0, tX1);
        }
    }

    private double getMaxY(double tY0, double tY1, double direction) {
        if (Math.abs(direction) < epsilon) {
            return Double.MAX_VALUE;
        } else {
            return Math.max(tY0, tY1);
        }
    }

    private double getMaxZ(double tZ0, double tZ1, double direction) {
        if (Math.abs(direction) < epsilon) {
            return Double.MAX_VALUE;
        } else {
            return Math.max(tZ0, tZ1);
        }
    }
    private boolean checkIfDirectionZeroIsOk(double sourceX, double sourceY, double sourceZ, Vec direction) {
        if((sourceX <a.x || sourceX > b.x) && direction.x ==0) return false;
        if((a.y > sourceY || sourceY > b.y) && direction.y ==0 ) return false;
        if((a.z > sourceZ || sourceZ > b.z) && direction.z == 0) return false;

        return true;

    }





}

