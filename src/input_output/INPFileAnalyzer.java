package input_output;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import Lists.DoubleLinkedListVector;
import Lists.TwoNodeVector;
import chemistry.JAtomTools;
import simulationTypes.Lattices;
import defaultPackage.IdealTetrahedron;
import defaultPackage.JAtom;
import defaultPackage.JVector;
import defaultPackage.Molecule;
import defaultPackage.Quaternion;

public class INPFileAnalyzer {

	private IdealTetrahedron[] lattice;
	
	private double aConstant;
	
	private int aUnits, numBromines;
	
	/**
	 * Constructor. 
	 * @param aFile
	 */
	public INPFileAnalyzer(String xyzORinp, File aFile)
	{/*
		if(xyzORinp.compareTo("xyz") == 0)
			makeLatticeFromXYZ(aFile);
		else if(xyzORinp.compareTo("inp") == 0)
			makeLatticeFromINP(aFile);*/
	}
	
	public INPFileAnalyzer() {
		// TODO Auto-generated constructor stub
	}

	public IdealTetrahedron[] makeLatticeFromXYZ(File inpFile) {
		IdealTetrahedron[] lattice;
		FileReader fr = null;

		Scanner s = null;
		
		int howManyLines = 0;
		
		try {
			fr = new FileReader(inpFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		s = new Scanner(fr);
		howManyLines = s.nextInt();

		s.nextLine();
		//s.nextLine();
		
		double aDim = 1;	// read in the dimension of the box
		numBromines = (howManyLines-2)*4/5;
		// define the lattice
		lattice = new IdealTetrahedron[(howManyLines-2)/5];

		// make the lattice
		//s.nextLine();	// skip the CELL line
		s.nextLine();	// skip the SPGP line

		for(int i = 0; i < lattice.length; i++)
		{
			JAtom C = convertFromXYZLine(s.nextLine());
			C.multiply(aDim);
			JAtom[] ligands = new JAtom[4];
			for(int j = 0; j < 4; j++)
			{
				ligands[j] = convertFromXYZLine(s.nextLine());
				ligands[j].multiply(aDim);
			}
			IdealTetrahedron toPlace = new IdealTetrahedron(C, ligands);
			
			lattice[i] = (IdealTetrahedron) toPlace.clone();
			
		}
		
		return lattice;
	}
	public JAtom convertFromXYZLine(String xyzLine) {
		String[] temp = xyzLine.split("\t");
		
		if(temp[0].isEmpty())
			return null;
		String abbrev;
		int Z = Integer.valueOf(temp[0]);
		if(Z == 6)
			abbrev = "C";
		else if(Z == 35)
			abbrev = "Br";
		else
			abbrev = "";
		
		double x = Double.valueOf(temp[1]);
		
		double y = Double.valueOf(temp[2]);
		
		double z = Double.valueOf(temp[3]);

		return new JAtom(Z, new JVector(x, y, z));
	}
	public IdealTetrahedron[] makeLatticeFromINP(File inpFile)
	{
		FileReader fr = null;

		Scanner s = null;
		
		boolean repeat = true;
		
		int howManyLines = 0;
		
		while(repeat)
		{
			try {
				fr = new FileReader(inpFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			s = new Scanner(fr);
			
			if(howManyLines == 0)
			{
				while(s.hasNextLine())
				{
					s.nextLine();
					howManyLines++;
				}
			}
			else repeat = false;
		}


		s.next(); // skip the CELL word
		
		double aDim = Double.valueOf(s.next());	// read in the dimension of the box
		// define the lattice
		lattice = new IdealTetrahedron[(howManyLines-2)/6];

		// make the lattice
		s.nextLine();	// skip the CELL line
		s.nextLine();	// skip the SPGP line

		for(int i = 0; i < lattice.length; i++)
		{
			JAtom C = convertFromInpLine(s.nextLine());
			C.multiply(aDim);
			JAtom[] ligands = new JAtom[4];
			for(int j = 0; j < 4; j++)
			{
				ligands[j] = convertFromInpLine(s.nextLine());
				ligands[j].multiply(aDim);
			}
			IdealTetrahedron toPlace = new IdealTetrahedron(C, ligands);
			
			lattice[i] = (IdealTetrahedron) toPlace.clone();
			
			s.nextLine();
		}
		
		return lattice;
	}
	
	private JAtom convertFromInpLine(String inpLine)
	{
		String[] temp = inpLine.split("\t");
		
		if(temp[0].isEmpty())
			return null;
		
		String abbrev = temp[0].toLowerCase();
		int Z = 0;
		if(abbrev.compareTo("c") == 0)
			Z = 6;
		if(abbrev.compareTo("br") == 0)
			Z = 35;
		
		double x = Double.valueOf(temp[1]);
		
		double y = Double.valueOf(temp[2]);
		
		double z = Double.valueOf(temp[3]);

		return new JAtom(Z, new JVector(x, y, z));
	}
	
	public Point2D.Double[] calculateClosenessTo110MainAngle()
	{
		int howManyOutside = 0;
		
		//int numBromines = (int) (Math.pow(aUnits / 2, 3) * 4 * 4); 
		
		Point2D.Double[] xy = new Point2D.Double[numBromines/4];
		
		Double[][] fourAngles = new Double[numBromines/4][4];
		
		JVector[][] allThree = make110Sets();
		
		int i = 0;
		int angleIndex = 0;
		for(int a = 0; a < lattice.length; a++)
		{
			JVector[] bromines = lattice[a].getVectors();
			DoubleLinkedListVector list = new DoubleLinkedListVector(12);
			// loop through the bromines
			for(int j = 1; j < bromines.length; j++)
			{
				bromines[j] = JVector.subtract(bromines[j], bromines[0]);

				// calculate which 110 is closest to the given bromine
				for(int k = 0; k < allThree.length; k++)
				{
					double angle = 0;
					try {
						angle = Math.acos(JVector.dot(bromines[j].unit(), allThree[k][0].unit())) * 180 / Math.PI;
					} catch (Exception e) {
						System.out.println("angle fail in INP analyzer");
					}
					TwoNodeVector temp = new TwoNodeVector(angle, k, null, null);
					temp.setKey2((j));
					list.insert(temp);
				}
			}
			TwoNodeVector closest = list.removeHead();
			
			//fourAngles[angleIndex][j-1] = closest.value;
			
			int whichVector = closest.getKey();
			int j = closest.getKey2();
			
			// determine the coordinates
			double xCoord = 0;
			try {
				xCoord = JVector.dot(bromines[j], allThree[whichVector][1].unit());
				if(xCoord > 500) {
					System.out.println(xCoord);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			double yCoord = 0;
			try {
				yCoord = JVector.dot(bromines[j], allThree[whichVector][2].unit());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(yCoord  > .2);
				//System.out.println("winner");
			
			//if(whichVector == 1)
			xy[i] = new Point2D.Double(xCoord, yCoord);
			angleIndex++;		
			i++;
		}
		/*
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("molecule_bromine-110_angles.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PrintStream ps = new PrintStream(fos);
		
		for(int j = 0; j < fourAngles.length; j++)
		{
			for(int k = 0; k < 4; k++)
			{
				ps.print(fourAngles[j][k] + "\t");
			}
			ps.println();
		}
		System.out.println(howManyOutside);
		*/
		return xy;
	}
	public Point2D.Double[][] calculateClosenessTo100AllFour() {
		return null;
		
	}
	public JVector[][] moveCarbonsToZero() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("angles.txt");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		PrintStream ps = new PrintStream(fos);
		JVector[][] set = JVector.axes110;
		JVector[][] newC = new JVector[lattice.length][2];
		JVector latticePos;
		TwoNodeVector[] keyValue = new TwoNodeVector[4];
		for(int i = 0; i < lattice.length; i++) {
			latticePos = lattice[i].getCenter().getPosition();
			latticePos = JVector.multiply(latticePos, 2./aConstant);
			latticePos = latticePos.roundInt();
			latticePos = JVector.multiply(latticePos, aConstant/2.);
			newC[i][0] = JVector.subtract(lattice[i].getCenter().getPosition(), (JVector) latticePos.clone());
			if(newC[i][0].length() > 2);
				//System.out.println("");
			JVector[] bromines = lattice[i].getVectors();
			
			// loop through the bromines
			for(int j = 1; j < bromines.length; j++)
			{
				bromines[j] = JVector.subtract(bromines[j], bromines[0]);
				DoubleLinkedListVector list = new DoubleLinkedListVector(set.length);

				// calculate which 110 is closest to the given bromine
				for(int k = 0; k < set.length; k++)
				{
					double angle = 0;
					try {
						angle = Math.acos(JVector.dot(bromines[j].unit(), set[k][0].unit())) * 180 / Math.PI;
					} catch (Exception e) {
						System.out.println("angle fail in INP analyzer");
					}
					TwoNodeVector temp = new TwoNodeVector(angle, k, null, null);
					temp.setKey2((j));
					list.insert(temp);
				}
				keyValue[j-1] = (TwoNodeVector) list.removeHead().clone();
			}
			TwoNodeVector max = new TwoNodeVector(180, 0, null, null);
			for(int j = 0; j < 4; j++) {
				if(keyValue[j].getValue() < max.getValue())
					max = keyValue[j];
			}
			newC[i][1] = (JVector) set[max.getKey()][0].clone();
			ps.println(JVector.angle(newC[i][0], newC[i][1]));
		}
		return newC;
	}
	public Point2D.Double[][] calculateClosenessToAllFour(JVector[][] set)
	{
		int howManyOutside = 0;
		
		//int numBromines = (int) (Math.pow(aUnits / 2, 3) * 4 * 4); 
		
		Point2D.Double[][] xy = new Point2D.Double[numBromines/4][4];
		Point2D.Double[] tempPoint = new Point2D.Double[4];
		Double[] tempVal = new Double[4];
		Double[][] fourAngles = new Double[numBromines/4][4];
		Double[] carbonAngles = new Double[numBromines/4];
		int i = 0;
		int angleIndex = 0;
		for(int a = 0; a < lattice.length; a++)
		{
			JVector[] bromines = lattice[a].getVectors();
			// loop through the bromines
			for(int j = 1; j < bromines.length; j++)
			{
				bromines[j] = JVector.subtract(bromines[j], bromines[0]);
				DoubleLinkedListVector list = new DoubleLinkedListVector(set.length);

				// calculate which 110 is closest to the given bromine
				for(int k = 0; k < set.length; k++)
				{
					double angle = 0;
					try {
						angle = Math.acos(JVector.dot(bromines[j].unit(), set[k][0].unit())) * 180 / Math.PI;
					} catch (Exception e) {
						System.out.println("angle fail in INP analyzer");
					}
					TwoNodeVector temp = new TwoNodeVector(angle, k, null, null);
					temp.setKey2((j));
					list.insert(temp);
				}

				TwoNodeVector closest = list.removeHead();
				
				fourAngles[angleIndex][j-1] = closest.getValue();
				
				int whichVector = closest.getKey();
				
				// determine the coordinates
				double xCoord = 0;
				try {
					xCoord = JVector.dot(bromines[j].unit(), set[whichVector][1].unit());
					if(xCoord > 500) {
						System.out.println("x:" + xCoord);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				double yCoord = 0;
				try {
					yCoord = JVector.dot(bromines[j].unit(), set[whichVector][2].unit());
					if(yCoord > 500) {
						System.out.println("y: " + yCoord);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(yCoord  > 5)
					System.out.println("winner");
				
				//if(whichVector == 1)
				tempVal[j-1] = fourAngles[angleIndex][j-1];
				tempPoint[j-1] = new Point2D.Double(xCoord, yCoord);
				tempVal[j-1] = fourAngles[angleIndex][j-1];
			}
			for(int j = 0; j < 4; j++) {
				int min = 0;
				double val = 500;
				for(int k = 0; k < 4; k++) {
					if(Math.abs(tempVal[k]) < val) { 
						val = Math.abs(tempVal[k]);
						min = k;
					}
				}
				tempVal[min] = 1000.;
				xy[i][j] = (Point2D.Double) tempPoint[min].clone();
			}
			i++;
			angleIndex++;		
	
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("molecule_bromine-110_angles.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PrintStream ps = new PrintStream(fos);
		
		for(int j = 0; j < fourAngles.length; j++)
		{
			for(int k = 0; k < 4; k++)
			{
				ps.print(fourAngles[j][k] + "\t");
			}
			ps.println();
		}
		System.out.println(howManyOutside);
		return xy;
	}

	public Double[] calculateNearestNeighborBromineCoorelation()
	{
		Double[] temp = new Double[1];
		
		return temp;
	}
	/*public IdealTetrahedron[] traverse110AndCalcAngles2()
	{
		IdealTetrahedron[] molecules = new IdealTetrahedron[100];
		
		JVector[][] allThree = make110Sets();

		int index = 0;
		
		int a = 5; int b = 5; int c = 4;

		IdealTetrahedron mol1 = lattice[a][b][c];
		IdealTetrahedron molPrev = mol1;

		boolean findNext = true;
		
		while(findNext == true)
		{
			JVector[] center = mol1.getVectors();
			
			DoubleLinkedListVector list = new DoubleLinkedListVector(12);

			for(int i = 0; i < allThree.length; i++)
			{
				int aCoord = (aUnits + a + (int)(allThree[i][0].getI())) % aUnits;
				int bCoord = (aUnits + b + (int)(allThree[i][0].getJ())) % aUnits;
				int cCoord = (aUnits + c + (int)(allThree[i][0].getK())) % aUnits;
				
				double angle = 90;
				
				IdealTetrahedron mol2 = lattice[aCoord][bCoord][cCoord];
				
				JVector[] ancillary = mol2.getVectors();
				
				for(int i_mol1 = 1; i_mol1 <= 4; i_mol1++)
				{
					for(int i_mol2 = 1; i_mol2 <= 4; i_mol2++)
					{
						double testAngle = 0;
						JVector br1 = JVector.subtract(center[i_mol1], center[0]);
						JVector br2 = JVector.subtract(ancillary[i_mol2], ancillary[0]);
						
						try {
							testAngle = Math.acos(JVector.dot(br1.unit(), br2.unit())) * 180 / Math.PI;
						} catch (Exception e) {
							System.out.println("angle fail in INP analyzer");
						}
						
						if(testAngle < angle) { angle = testAngle; }
					}
				}
				list.insert(new TwoNodeVector(angle, i, null, null));
			}
			boolean tryAgain = true;
			
			while(tryAgain == true)
			{
				TwoNodeVector head = list.removeHead();
				
				int whichDirection = head.key;

				molecules[index] = mol1;
				
				
				a = (aUnits + a + (int)(allThree[whichDirection][0].getI())) % aUnits;
				b = (aUnits + b + (int)(allThree[whichDirection][0].getJ())) % aUnits;
				c = (aUnits + c + (int)(allThree[whichDirection][0].getK())) % aUnits;
				
				
				if(lattice[a][b][c] == molPrev)
				{
					tryAgain = true;
				}
				else
				{
					tryAgain = false;
					molPrev = mol1;
					mol1 = lattice[a][b][c];
					index++;
				}
				
				// test to see if i've looped back on to myself
				for(int i = 0; i < index; i++)
				{
					if(mol1.getID() == molecules[i].getID())
						findNext = false;
				}
			}

		}
		
		return molecules;
	}

	public void traverse110AndCalcTotalAngle()
	{
		IdealTetrahedron[] molecules = new IdealTetrahedron[aUnits*aUnits*aUnits*2];
		
		JVector[][] allThree = make110Sets();
	
		int index = 0;
		
		int numCorrelationLength = 0;
		
		// position of the current ancillary molecule
		int aCoord = 0; int bCoord = 0; int cCoord = 0;
	
		//position of the current central molecule
		int aPos = 0; int bPos = 0; int cPos = 0;
		
		boolean findNext = true;
		
		for(int a = 0; a < lattice.length; a++)
		{
						aCoord = 0;
						bCoord = 0;
						cCoord = 0;
						aPos = 0;
						bPos = 0;
						cPos = 0;
						IdealTetrahedron mol1 = lattice[a];
						IdealTetrahedron molPrev = mol1;
						while(findNext == true)
						{

							JVector[] center = mol1.getVectors();
							
							DoubleLinkedListVector list = new DoubleLinkedListVector(12);
					
							for(int i = 0; i < allThree.length; i++)
							{
								aCoord = (aUnits + aPos + (int)(allThree[i][0].getI())) % aUnits;
								bCoord = (aUnits + bPos + (int)(allThree[i][0].getJ())) % aUnits;
								cCoord = (aUnits + cPos + (int)(allThree[i][0].getK())) % aUnits;
								
								double angle = 90;
								double totalAngle = 0;
								IdealTetrahedron mol2 = lattice[aCoord][bCoord][cCoord];
								
								JVector[] ancillary = mol2.getVectors();
								
								for(int i_mol1 = 1; i_mol1 <= 4; i_mol1++)
								{
									for(int i_mol2 = 1; i_mol2 <= 4; i_mol2++)
									{
										double testAngle = 0;
										JVector br1 = JVector.subtract(center[i_mol1], center[0]);
										JVector br2 = JVector.subtract(ancillary[i_mol2], ancillary[0]);
										
										try {
											testAngle = Math.acos(JVector.dot(br1.unit(), br2.unit())) * 180 / Math.PI;
										} catch (Exception e) {
											System.out.println("angle fail in INP analyzer");
										}
										
										if(testAngle < angle) { angle = testAngle; }
									}
									totalAngle += angle;
									angle = 90;
								}
								list.insert(new TwoNodeVector(totalAngle, i, null, null));
							}
							boolean tryAgain = true;
							boolean once = true;
							while(tryAgain == true)
							{
								TwoNodeVector head = list.removeHead();
								
								int whichDirection = head.key;
					
								molecules[index] = mol1;
								
								
								aPos = (aUnits + a + (int)(allThree[whichDirection][0].getI())) % aUnits;
								bPos = (aUnits + b + (int)(allThree[whichDirection][0].getJ())) % aUnits;
								cPos = (aUnits + c + (int)(allThree[whichDirection][0].getK())) % aUnits;
								
								
								if(lattice[aPos][bPos][cPos] == molPrev)
								{
									tryAgain = true;
								}
								else
								{
									//System.out.print(mol1.getID() + "\t" + allThree[whichDirection][0] + "\t" + Math.rint(head.value*1000)/1000);

									tryAgain = false;
									molPrev = mol1;
									mol1 = lattice[aPos][bPos][cPos];
									index++;
									numCorrelationLength++;
								}
								
								// test to see if i've looped back on to myself
								for(int i = 0; i < index; i++)
								{
									if(mol1.getID() == molecules[i].getID() && !once)
									{
										findNext = false;
									}
									else once = false;
								}
							}
						}
						System.out.println(numCorrelationLength);
						if(numCorrelationLength > 15)
							exportAtomsFile(molecules);
						numCorrelationLength = 0;
						index = 0;
						molecules = new IdealTetrahedron[100];
						findNext = true;
		}
		
	}
	*/
	private JVector[][] make111Sets() {
		JVector[][] v111s = new JVector[8][3];
		JVector axis = new JVector(0, 0, 1);
		double phi = 90;
		JVector origin = new JVector(0, 0, 0);
		v111s[0][0] = new JVector(1, 1, 1);
		v111s[0][1] = new JVector(1, -1, 0);
		v111s[0][2] = new JVector(1, 1, -2);
		
		for(int i = 1; i < 4; i++) {
			for(int j = 0; j < 3; j++) {
				v111s[i][j] = Quaternion.rotate(new Quaternion(v111s[0][j]), axis, origin, phi*i).getIJK();
			}
		}
		
		for(int i = 4; i < 8; i++) {
			v111s[i][1] = JVector.multiply(v111s[i-4][1], -1);
			v111s[i][2] = (JVector) v111s[i-4][2].clone();
			v111s[i][0] = JVector.cross(v111s[i][1], v111s[i][2]);
			
		}
		return v111s;
	}
	/**
	 * This method has projections <100> = <010> x <001>
	 * @return
	 */
	private JVector[][] make100Sets() {
		JVector[][] v100s = new JVector[6][3];
		JVector axis = new JVector(1, 1, 1);
		double phi = 120;
		JVector origin = new JVector(0, 0, 0);
		v100s[0][0] = new JVector(1, 0, 0);
		v100s[1][0] = new JVector(0, 1, 0);
		v100s[2][0] = new JVector(0, 0, 1);
		v100s[3][0] = new JVector(1, 0, 0);
		v100s[4][0] = new JVector(0, 1, 0);
		v100s[5][0] = new JVector(0, 0, 1);
		
		for(int i = 0; i < 6; i++) {
			v100s[i][1] = Quaternion.rotate(new Quaternion(v100s[i][0]), axis, origin, phi).getIJK();
			v100s[i][2] = Quaternion.rotate(new Quaternion(v100s[i][0]), axis, origin, -phi).getIJK();
			v100s[i][0] = JVector.cross(v100s[i][1], v100s[i][2]);
		}
		for(int i = 3; i < 6; i++) {
			v100s[i][1] = JVector.multiply(v100s[i-3][1], -1);
			v100s[i][0] = JVector.cross(v100s[i][1], v100s[i][2]);
		}
		return v100s;
	}
	private JVector[][] make110Sets()
	{
		JVector[][] allThree = new JVector[12][3];
		JVector[] twelve110 = new JVector[12];

		twelve110[0] = new JVector(1, 1, 0);
		twelve110[1] = new JVector(1, 0, 1);
		twelve110[2] = new JVector(0, 1, 1);
		twelve110[3] = new JVector(-1, 1, 0);
		twelve110[4] = new JVector(-1, 0, 1);
		twelve110[5] = new JVector(0, -1, 1);
		twelve110[6] = new JVector(1, -1, 0);
		twelve110[7] = new JVector(1, 0, -1);
		twelve110[8] = new JVector(0, 1, -1);
		twelve110[9] = new JVector(-1, -1, 0);
		twelve110[10] = new JVector(-1, 0, -1);
		twelve110[11] = new JVector(0, -1, -1);
		
		
		for(int i = 0; i < twelve110.length; i++)
		{
			allThree[i][0] = twelve110[i];
		}
		
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < twelve110.length; j++)
			{
				double dot = JVector.dot(twelve110[i], twelve110[j]);
				
				if(dot == 0)
				{
					JVector cross = JVector.cross(twelve110[i], twelve110[j]);
					if(cross.getI() >= 0 && cross.getJ() >= 0 && cross.getK() >= 0)
					{
						allThree[i][1] = twelve110[j];
						try {
							allThree[i][2] = cross.unit();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
					
			}
		}
		for(int i = 6; i < twelve110.length; i++)
		{
			allThree[i][1] = JVector.multiply(allThree[i-6][1], -1);
			allThree[i][2] = (JVector) allThree[i-6][2].clone();
			allThree[i][0] = JVector.cross(allThree[i][1], allThree[i][2]);
		}
		return allThree;
	}
	public String makeAtomsHeader()
	{
		String header = "";
		
		double a = aUnits * aConstant / 2;
		
		header += "CELL " + a + " " + a + " " + a + " " + 90 + " " + 90 + " " + 90 + "\n";
		header += "SPGP P1\n";
		
		return header;
	}
	public void exportAtomsFile(IdealTetrahedron[] molecules)
	{
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("analyzed_22.inp");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PrintStream ps = new PrintStream(fos);
		ps.println(makeAtomsHeader());
			for(int i = 0; i < molecules.length; i++)
		{
			if(molecules[i] != null)
				ps.println(molecules[i].toStringForAtoms(1, 1, 1, 0, 
					aUnits/2, aUnits/2, aUnits/2));
		}
	}
	public static void printVectorSets(JVector[][] set) {
		for(int i = 0; i < set.length; i++) {
			System.out.println(i + "\tz: " + set[i][0].roundInt() + "\tx: " + set[i][1].roundInt() + "\ty: " + set[i][2].roundInt());
		}
	}
	public static void printFourAngles(String fName, Point2D.Double[][] coords) {
		FileOutputStream fos = null, fosTotal = null;
		PrintStream ps = null, psTotal = null;
		try {
			fosTotal = new FileOutputStream(fName+"_total");
			psTotal = new PrintStream(fosTotal);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		// print each angle independently
		for(int j = 0; j < 4; j++) {
			try {
				fos = new FileOutputStream(fName+"_"+j);
				ps = new PrintStream(fos);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			for(int i = 0; i < coords.length; i++) {
				if(coords[i] != null) {
					ps.println(coords[i][j].x + "\t" + coords[i][j].y);
					psTotal.println(coords[i][j].x + "\t" + coords[i][j].y);
				}
			}
			ps.close();
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void addToLattice(IdealTetrahedron[] curLattice) {
		// copy the lattice to a temporary data structure
		int tempLatticeLength = 0;
		if(lattice != null) {
			tempLatticeLength = lattice.length;
			IdealTetrahedron[] tempLattice = new IdealTetrahedron[lattice.length];
			for(int i = 0; i < lattice.length; i++) {
				tempLattice[i] = (IdealTetrahedron) lattice[i].clone();
			}
			lattice = new IdealTetrahedron[tempLattice.length + curLattice.length];
			for(int i = 0; i < tempLattice.length; i++) {
				lattice[i] = (IdealTetrahedron) tempLattice[i].clone();
			}
			
		}else {
			lattice = new IdealTetrahedron[curLattice.length];
		}
		for(int i = 0; i < curLattice.length; i++) {
			lattice[i+tempLatticeLength] = (IdealTetrahedron) curLattice[i].clone();
		}
	}
	public void getLatticesFromFileList(File[] lattices, int numToRead, String xyzorinp) {
		IdealTetrahedron[] curFile;
		for(int i = 0; i < Math.min(numToRead, lattices.length); i++) {
			if(xyzorinp.compareTo("xyz")== 0) { curFile = makeLatticeFromXYZ(lattices[i]); }
			else { curFile = makeLatticeFromINP(lattices[i]); }
			addToLattice(curFile);
		}
		numBromines = lattice.length*4;
		System.out.println("Total number of bromines: " + numBromines);
	}
	public static void main(String[] args)
	{
		File findLoc = new File(".");
		
		String path = findLoc.getAbsolutePath();
		String xyzorinp = "xyz";
		path = path.substring(0, path.length()-1);

		File folder = new File("C:\\Users\\Eric\\Documents\\programming\\JNI\\diffraction.c\\afterSimul\\");
		folder = new File("C:\\Users\\JDM_5\\workspace\\JNI\\2ShellAfter\\");
		System.out.println(folder.getAbsolutePath());
		File[] lattices = folder.listFiles();
		
		//File[] lattices = {new File("C:\\Users\\JDM_5\\workspace\\CBr4Try3\\mono_out_2.inp")};
		System.out.println("Total number of files: " + lattices.length);
		INPFileAnalyzer test = new INPFileAnalyzer("inp", new File(path + "/outAfter.xyz"));
		test.aConstant = 8.82;
		test.getLatticesFromFileList(lattices, lattices.length, xyzorinp);
		System.out.println("100\n");
		printVectorSets(JVector.axes100);
		System.out.println("\n\n110\n");
		printVectorSets(JVector.axes110);
		System.out.println("\n\n111\n");
		printVectorSets(JVector.axes111);
		System.out.println("\n\n112\n");
		printVectorSets(JVector.axes112);
		int numProj = 4;
		JVector[][] axes = null;
		String fName = "ClosenessToProjection";
		for(int i = 0; i < numProj; i++) {
			String prefix = "1Shell_AfterSIMUL_";
			switch(i) {
			case 0: 	
				axes = JVector.axes100; 
				prefix += "100=010x001";
				break;
			case 1: 
				axes = JVector.axes110;
				prefix += "110=110x001";
				break;
			case 2: 
				axes = JVector.axes111;
				prefix += "111=110x112";
				break;
			case 3: 
				axes = JVector.axes112;
				prefix += "112=110x111";
				break;
			//case 3: axes = test.make112Sets(); break;
			}
			printFourAngles(prefix+fName, test.calculateClosenessToAllFour(axes));
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("cPos.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PrintStream ps = new PrintStream(fos); 
		JVector[][] newC = test.moveCarbonsToZero();
		for(int i = 0; i < newC.length; i++) {
			ps.print(newC[i][0].toString());
			ps.println("\t" + newC[i][1].toString());
		}
	}
}
