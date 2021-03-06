package edu.stanford.rsl.conrad.phantom.forbild.shapes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import edu.stanford.rsl.conrad.geometry.AbstractShape;
import edu.stanford.rsl.conrad.geometry.Axis;
import edu.stanford.rsl.conrad.geometry.CoordinateSystem;
import edu.stanford.rsl.conrad.geometry.General;
import edu.stanford.rsl.conrad.geometry.Rotations;
import edu.stanford.rsl.conrad.geometry.bounds.HalfSpaceBoundingCondition;
import edu.stanford.rsl.conrad.geometry.shapes.StandardCoordinateSystem;
import edu.stanford.rsl.conrad.geometry.shapes.simple.Cylinder;
import edu.stanford.rsl.conrad.geometry.shapes.simple.Plane3D;
import edu.stanford.rsl.conrad.geometry.shapes.simple.PointND;
import edu.stanford.rsl.conrad.geometry.transforms.AffineTransform;
import edu.stanford.rsl.conrad.numerics.SimpleMatrix;
import edu.stanford.rsl.conrad.numerics.SimpleVector;
import edu.stanford.rsl.conrad.numerics.mathexpressions.Evaluator;


/**
 * <p>This class creates a surface from a <a href = "http://www.imp.uni-erlangen.de/forbild/english/forbild/index.htm">forbild</a>definition.</p>
 * <p>The expression [Cylinder: x=originX;  y=originY;  z=originZ;  r = radius;  dz= height; axis(a_x, a_y, a_z)] defines a uniform cylinder with axis (a_x, a_y, a_z) with center of mass at (originX,originY,originZ),.</p>
 *
 * @author Rotimi .X. Ojo
 */
public class ForbildCylinder extends Cylinder {	
	
	public static final long serialVersionUID = 7086658338878039459L;
	
	private double dx,dy,dz;
	private PointND surfaceOrigin = new PointND(0,0,0);
	private SimpleVector axis = new SimpleVector(0,0,1);
	private SimpleVector a_x,a_y;
	private ArrayList<Plane3D> boundingPlanes =  new ArrayList<Plane3D>();

	public ForbildCylinder(String expression){
		parseExpression(expression);
		correctAxis(expression);
		InitializeCylinder();
		correctAndAddBoundingConditions();
	}
	
	public ForbildCylinder(ForbildCylinder fc){
		super(fc);
		dx = fc.dx;
		dy = fc.dy;
		dz = fc.dz;
		surfaceOrigin = (fc.surfaceOrigin != null) ? fc.surfaceOrigin.clone() : null;
		axis = (fc.axis != null) ? fc.axis.clone() : null;
		if (fc.boundingPlanes != null){
			boundingPlanes = new ArrayList<Plane3D>();
			Iterator<Plane3D> it = fc.boundingPlanes.iterator();
			while (it.hasNext()) {
				Plane3D pl = it.next();
				boundingPlanes.add((pl!=null) ? new Plane3D(pl) : null);
			}
		}
		else{
			boundingPlanes = null;
		}
		
		a_x = (fc.a_x != null) ? fc.a_x.clone() : null;
		a_y = (fc.a_y != null) ? fc.a_y.clone() : null;
	}

	/**
	 * Calculates the affine transform that transforms an object object space to world space
	 */
	private void InitializeCylinder() {
		SimpleMatrix scaleRotate = null;
		if(a_x != null || a_y != null){
			CoordinateSystem oldSystem = new StandardCoordinateSystem(getDimension());
			CoordinateSystem newSystem = createNewCoodSystem();						
			scaleRotate = Rotations.create3DChangeOfAxesMatrix(oldSystem, newSystem);
		}else{
			scaleRotate = getChangeOfAxisMatrix(new Axis(axis));
		}
		SimpleVector translatorVec = surfaceOrigin.getAbstractVector();
		AffineTransform tr = new AffineTransform(scaleRotate, translatorVec);
		super.init(dx, dy, dz, tr);		
	}	
	
	private CoordinateSystem createNewCoodSystem() {
		SimpleVector a_z = completeTriaxis();
		/*SimpleMatrix rot = new SimpleMatrix(3,3);
		rot.setRowValue(0, a_x.getSubVec(0, a_x.getLen()));
		rot.setRowValue(1, a_y.getSubVec(0, a_y.getLen()));
		rot.setRowValue(2, a_z.getSubVec(0, a_z.getLen()));
		rot.transpose();
		return rot;*/
		return new CoordinateSystem(new Axis(a_x), new Axis(a_y), new Axis(a_z));
	}

	private SimpleVector completeTriaxis() {

		SimpleVector a_z = (axis==null) ? new SimpleVector(0,0,1) : axis;
		if(a_x != null && a_y == null)
			a_y = General.crossProduct(a_x, a_z);
		else if(a_y != null && a_x == null)
			a_x = General.crossProduct(a_y, a_z);
		else
			throw new RuntimeException();
		return a_z;
	}
	
	/**
	 * Moves bounding planes from world space to the space of the bounded object
	 * Creates a bounding condition using this new plane and updates the superclass
	 */
	private void correctAndAddBoundingConditions() {
		Iterator<Plane3D> it = boundingPlanes.iterator();
		while(it.hasNext()){
			Plane3D currPlane = it.next();
			currPlane.applyTransform(transform.inverse());
			addBoundingCondition(new HalfSpaceBoundingCondition(currPlane));
		}		
	}


	private void parseExpression(String expression) {
		expression = expression.trim();
		if(expression.charAt(0)=='(' && expression.charAt(expression.length()-1)==')'){
			expression = expression.substring(1,expression.length()-1);
		}	
		
		String props = expression.substring(expression.indexOf(':')+ 1).trim();
		
		Scanner sc = new Scanner(props);
		sc.useDelimiter(";");		
		while(sc.hasNext()){
			String currProp = sc.next().trim();
			if(currProp.charAt(0)== 'x'&& currProp.contains("=")){
				surfaceOrigin.set(0,Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1)));
			}else if(currProp.charAt(0)== 'y'&& currProp.contains("=")){
				surfaceOrigin.set(1,Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1)));
			}else if(currProp.charAt(0)== 'z' && currProp.contains("=")){
				surfaceOrigin.set(2,Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1)));
			}else if(currProp.indexOf("dx")==0){
				dx = Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1));
			}else if(currProp.indexOf("dy")==0){
				dy = Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1));
			}else if(currProp.charAt(0)== 'r' && !(currProp.contains(">") || currProp.contains("<"))){
				double radius = Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1));
				dx = radius;
				dy = radius;
			}else if(currProp.charAt(0)== 'l'){
				dz = Evaluator.getValue(currProp.substring(currProp.indexOf('=')+1));
			}else if(currProp.contains("axis")){
				axis = Evaluator.getVectorValue(currProp.substring(currProp.indexOf('('), currProp.length()));
			}else if(currProp.contains("r") && (currProp.contains(">") || currProp.contains("<"))){				
				boundingPlanes.add(Evaluator.getPlane(currProp));
			}else if(currProp.indexOf("a_x")==0){
				a_x = Evaluator.getVectorValue(currProp.substring(currProp.indexOf('('), currProp.length()));
			}else if(currProp.indexOf("a_y")==0){
				a_y = Evaluator.getVectorValue(currProp.substring(currProp.indexOf('('), currProp.length()));
			}else if(currProp.contains("x") && (currProp.contains(">") || currProp.contains("<"))){
				String newProp = "";
				if(currProp.contains(">") ){
					newProp = "r ( -1 , 0 , 0 )"+currProp.substring(currProp.indexOf(">"));
				}else{
					newProp = "r ( 1 , 0 , 0 )"+currProp.substring(currProp.indexOf("<"));
				}
				boundingPlanes.add(Evaluator.getPlane(newProp));
			}else if(currProp.contains("y") && (currProp.contains(">") || currProp.contains("<"))){
				String newProp = "";
				if(currProp.contains(">") ){
					newProp = "r ( 0 , -1 , 0 )"+currProp.substring(currProp.indexOf(">"));
				}else{
					newProp = "r ( 0 , 1 , 0 )"+currProp.substring(currProp.indexOf("<"));
				}
				boundingPlanes.add(Evaluator.getPlane(newProp));
			}else if(currProp.contains("z") && (currProp.contains(">") || currProp.contains("<"))){
				String newProp = "";
				if(currProp.contains(">") ){
					newProp = "r ( 0 , 0 , -1 )"+currProp.substring(currProp.indexOf(">"));
				}else{
					newProp = "r ( 0 , 0 , 1 )"+currProp.substring(currProp.indexOf("<"));
				}
				boundingPlanes.add(Evaluator.getPlane(newProp));
			}
		}
	}
	
	/**
	 * Determines the subclass of forbild cylinder and corrects axis information appropriately
	 * @param objectType
	 */
	private void correctAxis(String expression) {
		String objectType = expression.substring(0, expression.indexOf(':'))
		.trim().toLowerCase();
		
		int length = objectType.length();
		if(objectType.charAt(length-2)== '_' && objectType.charAt(length - 1)=='x'){
			axis = new SimpleVector(1,0,0);
		}else if(objectType.charAt(length-2)== '_' && objectType.charAt(length - 1)=='y'){
			axis = new SimpleVector(0,1,0);
		}else if(objectType.charAt(length-2)== '_' && objectType.charAt(length - 1)=='z'){
			axis = new SimpleVector(0,0,1);
		}
	}

	
	@Override
	public AbstractShape clone() {
		return new ForbildCylinder(this);
	}
}
/*
 * Copyright (C) 2010-2014 Rotimi X Ojo
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/