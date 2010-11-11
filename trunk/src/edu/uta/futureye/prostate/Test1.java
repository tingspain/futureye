package edu.uta.futureye.prostate;

import java.util.HashMap;

import edu.uta.futureye.core.*;
import edu.uta.futureye.core.intf.Point;
import edu.uta.futureye.algebra.*;
import edu.uta.futureye.function.*;
import edu.uta.futureye.function.basic.*;
import edu.uta.futureye.function.intf.Function;
import edu.uta.futureye.function.operator.FOBasic;
import edu.uta.futureye.function.operator.FOBasicDerivable;
import edu.uta.futureye.function.shape.SFLinearLocal2D;
import edu.uta.futureye.io.*;
import edu.uta.futureye.test.VectorBasedFunction;
import edu.uta.futureye.util.*;

public class Test1 {
	public static void allTest() {
		MeshReader reader = new MeshReader("prostate_test1.grd");
		Mesh mesh = reader.read2D();
		mesh.computeNodesBelongToElement();
		
		//Mark border type
		HashMap<NodeType, Function> mapNTF = new HashMap<NodeType, Function>();
		mapNTF.put(NodeType.Robin, null);
		
		//mapNTF.put(NodeType.Dirichlet, null);		
		
//		mapNTF.put(NodeType.Dirichlet, new FAbstract("x","y"){
//			@Override
//			public double value(Variable v) {
//				//if(Math.abs(v.get("y"))<0.01 || Math.abs(v.get("x")-5.0)<0.01)
//					if(Math.abs(v.get("y"))<0.01)
//					return 1.0;
//				else
//					return -1.0;
//			}
//		});
		
		
		mesh.markBorderNode(mapNTF);

		SFLinearLocal2D[] shapeFun = new SFLinearLocal2D[3];
		shapeFun[0] = new SFLinearLocal2D(1);
		shapeFun[1] = new SFLinearLocal2D(2);
		shapeFun[2] = new SFLinearLocal2D(3);
		
		//Asign degree of freedom to element
		for(int i=1;i<=mesh.getElementList().size();i++) {
			Element e = mesh.getElementList().at(i);
			for(int j=1;j<=e.nodes.size();j++) {
				//Asign shape function to DOF
				DOF dof = new DOF(j,e.nodes.at(j).globalIndex,shapeFun[j-1]);
				e.addDOF(j, dof);
			}
		}
		
		//User defined weak form of PDE (including bounder conditions)
		WeakFormLaplace2D weakForm = new WeakFormLaplace2D();
		//Right hand side
		Variable x0 = new Variable();
		x0.set("x", 1.0);
		x0.set("y", 2.8);
		FDelta delta = new FDelta(x0,0.01,2e5);
		weakForm.setF(delta);
		
		final double cx[] = {2.0, 3.0, 4.0};
		double cy[] = {1.5, 2.0, 2.5};
		double mu_a[] = {0.1,0.2,0.3,0.4,1.0};
		for(int cxi=0;cxi<cx.length;cxi++)
			for(int cyi=0;cyi<cy.length;cyi++)
				for(int mu_ai=0;mu_ai<mu_a.length;mu_ai++) {
			
			final double fcx = cx[cxi];
			final double fcy = cy[cyi];
			final double fmu_a = mu_a[mu_ai];
									
			weakForm.setParam(
			new FConstant(0.02), //  1/(3*mu_s') = 0.02
			new FAbstract("x","y"){ //mu_a
				@Override
				public double value(Variable v) {
					double dx = v.get("x")-fcx;
					double dy = v.get("y")-fcy;
					if(Math.sqrt(dx*dx+dy*dy)<0.5)
						return fmu_a; 
					else
						return 0.1;
				}
			},
			new FConstant(0.05),null //Robin: d*u + k*u_n = q
			); 
			
			Assembler assembler = new Assembler(mesh, weakForm);
			System.out.println("Begin Assemble...");
			Matrix stiff = assembler.getStiffnessMatrix();
			Vector load = assembler.getLoadVector();
			assembler.imposeDirichletCondition(new FConstant(0.0));
			System.out.println("Assemble done!");
			
			Solver solver = new Solver();
	//		Matrix stiff2 = new Matrix(stiff.getRowDim()+1,stiff.getColDim()+1);
	//		Vector load2 = new Vector(load.getDim()+1);
	//		for(int i=1;i<=stiff.getColDim();i++) {
	//			for(int j=1;j<=stiff.getRowDim();j++)
	//				stiff2.set(i, j, stiff.get(i, j));
	//			load2.set(i, load.get(i));
	//		}
	//		for(int i=1;i<=stiff2.getColDim();i++) {
	//			stiff2.set(stiff2.getRowDim(), i, 1.0);
	//			stiff2.set(i, stiff2.getColDim(), 1.0);
	//		}
	//		load2.set(load2.getDim(), 1000000);
	//		Vector u = solver.solve(stiff2, load2);
			
			
	//		for(int i=1;i<=stiff.getColDim();i++) {
	//			double a = stiff.get(1, i);
	//			stiff.plusValue(2, i, a);
	//			stiff.set(1, i, 1.0);
	//		}
	//		load.plusValue(2, load.get(1));
	//		load.set(1, 1000);
			
			Vector u = solver.solve(stiff, load);
		    
			System.out.println("u=");
		    for(int i=1;i<=u.getDim();i++)
		        System.out.println(String.format("%.3f", u.get(i)));	
		    
		    MeshWriter writer = new MeshWriter(mesh);
		    writer.writeTechplot("prostate_test1"+String.format("_%d_%d_%d", cxi,cyi,mu_ai)+".dat", 
		    		u);
		}
		
	}

	
	public static void paramInverseTest() {
		MeshReader reader = new MeshReader("prostate_test1.grd");
		Mesh mesh = reader.read2D();
		mesh.computeNodesBelongToElement();
		
		//Mark border type
		HashMap<NodeType, Function> mapNTF = new HashMap<NodeType, Function>();
		mapNTF.put(NodeType.Robin, null);
		mesh.markBorderNode(mapNTF);

		SFLinearLocal2D[] shapeFun = new SFLinearLocal2D[3];
		shapeFun[0] = new SFLinearLocal2D(1);
		shapeFun[1] = new SFLinearLocal2D(2);
		shapeFun[2] = new SFLinearLocal2D(3);
		
		//Asign degree of freedom to element
		for(int i=1;i<=mesh.getElementList().size();i++) {
			Element e = mesh.getElementList().at(i);
			for(int j=1;j<=e.nodes.size();j++) {
				//Asign shape function to DOF
				DOF dof = new DOF(j,e.nodes.at(j).globalIndex,shapeFun[j-1]);
				e.addDOF(j, dof);
			}
		}
		
		//User defined weak form of PDE (including bounder conditions)
		WeakFormLaplace2D weakForm = new WeakFormLaplace2D();
		//Right hand side
		Variable x0 = new Variable();
		x0.set("x", 1.0);
		x0.set("y", 2.8);
		FDelta delta = new FDelta(x0,0.01,2e5);
		weakForm.setF(delta);
		
		final double fcx = 2.0;
		final double fcy = 2.5;
		final double fmu_a = 1.0;

		weakForm.setParam(
		new FConstant(0.02), //  1/(3*mu_s') = 0.02
		new FAbstract("x","y"){ //mu_a
			@Override
			public double value(Variable v) {
				double dx = v.get("x")-fcx;
				double dy = v.get("y")-fcy;
				if(Math.sqrt(dx*dx+dy*dy)<0.5)
					return fmu_a; 
				else
					return 0.1;
			}
		},
		new FConstant(0.05),null //Robin: d*u + k*u_n = q
		); 
		
		Assembler assembler = new Assembler(mesh, weakForm);
		System.out.println("Begin Assemble...");
		Matrix stiff = assembler.getStiffnessMatrix();
		Vector load = assembler.getLoadVector();
		assembler.imposeDirichletCondition(new FConstant(0.0));
		System.out.println("Assemble done!");
		
		Solver solver = new Solver();
		Vector u = solver.solve(stiff, load);
		System.out.println("u=");
	    for(int i=1;i<=u.getDim();i++)
	        System.out.println(String.format("%.3f", u.get(i)));	
	    
	    MeshWriter writer = new MeshWriter(mesh);
	    writer.writeTechplot("prostate_test1"+ String.format("_u.dat"), u);
	    
	    
		//User defined weak form of PDE (including bounder conditions)
		HashMap<NodeType, Function> mapNTF2 = new HashMap<NodeType, Function>();
		mapNTF2.put(NodeType.Dirichlet, null);
		mesh.clearBorderNodeMark();
		mesh.markBorderNode(mapNTF2);
		
		WeakFormL22D weakFormL2 = new WeakFormL22D();
		weakFormL2.setF(delta);
		weakFormL2.setParam(
				new FConstant(0.02), 
				new VectorBasedFunction(u)
				);
		
		Assembler assembler2 = new Assembler(mesh, weakFormL2);
		System.out.println("Begin Assemble...");
		Matrix stiff2 = assembler2.getStiffnessMatrix();
		Vector load2 = assembler2.getLoadVector();
		assembler2.imposeDirichletCondition(new FConstant(0.1));
		System.out.println("Assemble done!");
		
		Solver solver2 = new Solver();
		Vector u2 = solver2.solve(stiff2, load2);
		System.out.println("alpha=");
	    for(int i=1;i<=u2.getDim();i++)
	        System.out.println(String.format("%.3f", u2.get(i)));	

	    writer.writeTechplot("prostate_test1"+String.format("_alpha.dat"), u2);
	}
	
	public static void main(String[] args) {
		Model model = new Model();
				
		//3*5 => 30*50
		//model.run(1,1,"prostate_test1");
		//3*5 => 10*15
		//model.run(1,2,"prostate_test2");
		//3*5 => 15*25
		//model.run(1,3,"prostate_test3");
		//3*5 => manual adaptive
		//model.run(1,4,"prostate_test4_linear");
		//model.run(2,4,"prostate_test4_quadratic");
		//3*5 => mixed
		//model.run(1,5,"prostate_test5_mixed");
		//3*5 => rectangle
		//model.run(1,6,"prostate_test6_rectangle");
		
		
		//model.runAdaptive(1,1,"prostate_test1");
		//model.runAdaptive(1,2,"prostate_test2");
		model.runAdaptive(1,3,"prostate_test3");
		//model.runAdaptive(1,2,"prostate_test2");
		//model.runAdaptive(1,6,"prostate_test6_rectangle");
		//model.runAdaptive(1,7,"prostate_test7_rectangle");
		
		//model.runAdaptive(1,5,"prostate_test5_mixed");
	
	}
	
}
