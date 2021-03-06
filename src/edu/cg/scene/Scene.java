package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; //gets the values of 1, 2 and 3
	private boolean renderRefractions = false;
	private boolean renderReflections = false;
	
	private PinholeCamera camera;
	private Vec ambient = new Vec(0.1, 0.1, 0.1); //white
	private Vec backgroundColor = new Vec(0, 0.5, 1); //blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();
	
	
	//MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec,  double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec,  distanceToPlain);
		return this;
	}

	public Scene initCamera(PinholeCamera pinholeCamera) {
		this.camera = pinholeCamera;
		return this;
	}
	
	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}
	
	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}
	
	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}
	
	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}
	
	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}
	
	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}
	
	public Scene initName(String name) {
		this.name = name;
		return this;
	}
	
	public Scene initRenderRefractions(boolean renderRefractions) {
		this.renderRefractions = renderRefractions;
		return this;
	}
	
	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}
	
	//MARK: getters
	public String getName() {
		return name;
	}
	
	public int getFactor() {
		return antiAliasingFactor;
	}
	
	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}
	
	public boolean getRenderRefractions() {
		return renderRefractions;
	}
	
	public boolean getRenderReflections() {
		return renderReflections;
	}
	
	@Override
	public String toString() {
		String endl = System.lineSeparator(); 
		return "Camera: " + camera + endl +
				"Ambient: " + ambient + endl +
				"Background Color: " + backgroundColor + endl +
				"Max recursion level: " + maxRecursionLevel + endl +
				"Anti aliasing factor: " + antiAliasingFactor + endl +
				"Light sources:" + endl + lightSources + endl +
				"Surfaces:" + endl + surfaces;
	}
	
	private transient ExecutorService executor = null;
	private transient Logger logger = null;

	// TODO: add your fields here with the transient keyword
	//  for example - private transient Object myField = null;

	private void initSomeFields(int imgWidth, int imgHeight, double planeWidth, Logger logger) {
		this.logger = logger;
		// TODO: initialize your fields that you added to this class here.
		//      Make sure your fields are declared with the transient keyword
	}
	
	
	public BufferedImage render(int imgWidth, int imgHeight, double planeWidth ,Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {
		
		initSomeFields(imgWidth, imgHeight, planeWidth, logger);
		
		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, planeWidth);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Initialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);
		
		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][])(new Future[imgHeight][imgWidth]);
		
		this.logger.log("Starting to shoot " +
			(imgHeight*imgWidth*antiAliasingFactor*antiAliasingFactor) +
			" rays over " + name);
		
		for(int y = 0; y < imgHeight; ++y)
			for(int x = 0; x < imgWidth; ++x)
				futures[y][x] = calcColor(x, y);
		
		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");
		
		for(int y = 0; y < imgHeight; ++y)
			for(int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}
		
		executor.shutdown();
		
		this.logger.log("Ray tracing of " + name + " has been completed.");
		
		executor = null;
		this.logger = null;
		
		return img;
	}
	
	private Future<Color> calcColor(int x, int y) {
		return executor.submit(() -> {
			Point pointOnScreen = camera.transform(x, y);
			Vec color = new Vec(0.0);

			Ray ray = new Ray(camera.getCameraPosition(), pointOnScreen);
			color = color.add(calcColor(ray, 0));
//			System.out.println(color.toString());
			return color.toColor();
			// TODO: change this method for AntiAliasing bonus
			//		You need to shoot antiAliasingFactor-1 additional rays through the pixel return the average color of
			//      all rays.
		});
	}

	private Vec calcColor(Ray ray, int recursionLevel) {
		if(recursionLevel > this.maxRecursionLevel){
			return new Vec(0.0, 0.0,0.0);
		}else{
//			System.out.println("hello");
			Hit closestHit = getClosestHit(ray);
			if (closestHit.getNormalToSurface() == null){
				return this.backgroundColor;
			}
			Point closestHittingPoint = ray.getHittingPoint(closestHit);
			Surface surfaceHit = closestHit.getSurface();

			Vec colour = this.ambient.mult(surfaceHit.Ka());

			colour = Ops.add(colour, getCurrentColour(closestHittingPoint, closestHit, ray));
			//add ambient colour
//			System.out.println(colour.toString());
			if (renderReflections && surfaceHit.isReflecting()){
				Vec reflectedVector = getReflectionVector(ray, closestHit);

				return Ops.add(Ops.mult(surfaceHit.Kr(), calcColor(new Ray(closestHittingPoint, reflectedVector), recursionLevel + 1)), colour);

			}else{
				return colour;
			}

			}
		}

	private Vec getReflectionVector(Ray ray, Hit closestHit) {
		return Ops.reflect(ray.direction(), closestHit.getNormalToSurface());
	}

	private Hit getClosestHit(Ray ray) {
		Hit closestHit = new Hit(Double.MAX_VALUE,null);
		Hit currentHit = null;
		for (Surface surface: surfaces) {
			currentHit = surface.intersect(ray);
			if(currentHit != null && currentHit.t() <= closestHit.t() && currentHit.t() > 0){
				closestHit = currentHit;
			}
		}
		return closestHit;
	}

	private Vec getCurrentColour(Point point, Hit currentHit, Ray prevRay) {
		Vec colour = new Vec(0,0,0);
		boolean isBlocked = false;
		for (Light lightSource: lightSources) {
			Ray rayToLight = lightSource.rayToLight(point);
			isBlocked = checkIfBlocked(rayToLight, lightSource);
			if(!isBlocked){

				Vec diffuseColor = getDiffuseColor(currentHit,rayToLight);
				Vec specularColor = getSpecularColor(currentHit,rayToLight,prevRay);
				Vec currentColour = Ops.mult((Ops.add(diffuseColor, specularColor)), lightSource.intensity(point,rayToLight));
				colour = Ops.add(colour, currentColour);
			}
		}
		return colour;
		//loop all light sources and culc colour of only not blocked sources
	}

	private Vec getDiffuseColor(Hit hit, Ray rayToLight) {
		Vec kd = hit.getSurface().Kd();
		Vec normalToSurface = hit.getNormalToSurface();
		Vec rayToLightDirection = rayToLight.direction();
		double innerProduct = normalToSurface.dot(rayToLightDirection);
		return kd.mult(innerProduct);

	}

	private Vec getSpecularColor(Hit hit, Ray rayToLight, Ray prevRay) {
		Vec ks = hit.getSurface().Ks();
		Vec reflectedRay = getReflectionVector(rayToLight,hit);
		Vec prevRayDirection = prevRay.direction();
		double innerProduct = Math.pow(prevRayDirection.dot(reflectedRay), hit.getSurface().shininess());
		return ks.mult(innerProduct);
	}


	private boolean checkIfBlocked(Ray rayToLight, Light lightSource) {
		boolean isBlocked = false;
		for (Surface surface:surfaces) {
			if(lightSource.isOccludedBy(surface, rayToLight)){
				isBlocked = true;
				break;
			}
		}

		return isBlocked;
	}
}
