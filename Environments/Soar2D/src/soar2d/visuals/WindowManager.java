package soar2d.visuals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import soar2d.Direction;
import soar2d.Names;
import soar2d.Soar2D;
import soar2d.config.SimConfig;
import soar2d.map.BookMap;
import soar2d.map.CellObject;
import soar2d.map.EatersMap;
import soar2d.map.GridMap;
import soar2d.map.KitchenMap;
import soar2d.map.TankSoarMap;
import soar2d.map.TaxiMap;
import soar2d.players.MoveInfo;
import soar2d.players.Player;

public class WindowManager {
	private static Logger logger = Logger.getLogger(WindowManager.class);

	public static Color white = null;
	public static Color widget_background = null;
	public static Color blue = null;
	public static Color red = null;
	public static Color yellow = null;
	public static Color orange = null;
	public static Color black = null;
	public static Color green = null;
	public static Color purple = null;
	public static Color brown = null;
	public static Color lightGray = null;
	public static Color darkGray = null;

	static Display display;
	protected Shell shell;
	Label scoreCount;
	Label foodCount;
	Label worldCount;
	SimulationButtons simButtons;
	MapButtons mapButtons;
	VisualWorld visualWorld;
	AgentDisplay agentDisplay;
	Group worldGroup;
	Label statusLine;
	String popUpTitle;
	String popUpMessage;
	String statusMessage;
	MoveInfo humanMove;
	Player human;
	Composite rhs;
	Composite currentSide;

	public static final int kEatersMainMapCellSize = 20;
	public static final int kTaxiMainMapCellSize = 20;
	public static final int kKitchenMainMapCellSize = 20;
	public static final int kTanksoarMainMapCellSize = 32;
	public static final String kFoodRemaining = "Food remaining: ";
	public static final String kScoreRemaining = "Points remaining: ";
	public static final String kWorldCount = "World count: ";

	private static void initColors(Display d) {
	    white = d.getSystemColor(SWT.COLOR_WHITE);
		widget_background = d.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		blue = d.getSystemColor(SWT.COLOR_BLUE);
		red = d.getSystemColor(SWT.COLOR_RED);
		yellow = d.getSystemColor(SWT.COLOR_YELLOW);
		green = d.getSystemColor(SWT.COLOR_GREEN);
		purple = d.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		orange = new Color(d, 255, 127, 0);
		black = d.getSystemColor(SWT.COLOR_BLACK);
		brown = new Color(d, 128, 64, 0);
		lightGray = new Color(d, 170, 170, 170);
		darkGray = new Color(d, 100, 100, 100);
	}
	
	public static Color getColor(String color) {
		if (color == null) {
			return null;
		}
		if (color.equalsIgnoreCase("white")) {
			return white;
		}
		if (color.equalsIgnoreCase("blue")) {
			return blue;
		}
		if (color.equalsIgnoreCase("red")) {
			return red;
		}
		if (color.equalsIgnoreCase("yellow")) {
			return yellow;
		}
		if (color.equalsIgnoreCase("green")) {
			return green;
		}
		if (color.equalsIgnoreCase("purple")) {
			return purple;
		}
		if (color.equalsIgnoreCase("orange")) {
			return orange;
		}
		if (color.equalsIgnoreCase("black")) {
			return black;
		}
		if (color.equalsIgnoreCase("brown")) {
			return brown;
		}
		if (color.equalsIgnoreCase("lightGray")) {
			return lightGray;
		}
		if (color.equalsIgnoreCase("darkGray")) {
			return darkGray;
		}
		return null;
	}

	public boolean initialize() {
		// can be called multiple times
		if (display != null) {
			return true;
		}

		try {
			display = new Display();
		} catch (java.lang.UnsatisfiedLinkError e) {
			logger.info("Failed to create display (java.lang.UnsatisfiedLinkError), this is normal on systems that do not have a window manager.");
			return false;
		} catch (org.eclipse.swt.SWTError e) {
			logger.info("Failed to create display (org.eclipse.swt.SWTError), this is expected if there is no window manager available.");
			return false;
		}
		assert display != null;

		shell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.TITLE);
		if (shell == null) {
			display.dispose();
			display = null;
			return false;		}
		initColors(display);
		
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				// save window position
				if (shell != null) {
					int [] xy = new int[2];
					xy[0] = shell.getLocation().x;
					xy[1] = shell.getLocation().y;
					if (Soar2D.config != null) {
						Soar2D.config.saveWindowPosition(xy);
					}
				}
			}
		});
		return true;
	}
	
	public boolean using() {
		if (Soar2D.config == null) {
			return false;
		}
		return !Soar2D.config.generalConfig().headless;
	}
	
	public void setupEaters() {
		worldGroup = new Group(shell, SWT.NONE);
		worldGroup.setLayout(new FillLayout());
		visualWorld = new EatersVisualWorld(worldGroup, SWT.NONE, kEatersMainMapCellSize);
		visualWorld.setMap(Soar2D.simulation.world.getMap());

		visualWorld.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				Player player = visualWorld.getPlayerAtPixel(new int [] { e.x, e.y });
				if (player == null) {
					return;
				}
				agentDisplay.selectPlayer(player);
			}
		});
		visualWorld.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (humanMove == null) {
					return;
				}
				boolean go = false;
				switch (e.keyCode) {
				case SWT.KEYPAD_8:
					humanMove.move = true;
					humanMove.moveDirection = Direction.NORTH;
					go = true;
					break;
				case SWT.KEYPAD_6:
					humanMove.move = true;
					humanMove.moveDirection = Direction.EAST;
					go = true;
					break;
				case SWT.KEYPAD_2:
					humanMove.move = true;
					humanMove.moveDirection = Direction.SOUTH;
					go = true;
					break;
				case SWT.KEYPAD_4:
					humanMove.move = true;
					humanMove.moveDirection = Direction.WEST;
					go = true;
					break;
				case SWT.KEYPAD_5:
					humanMove.jump = false;
					go = true;
					break;
				case SWT.KEYPAD_0:
					humanMove.jump = !humanMove.jump;
					break;
				case SWT.KEYPAD_DECIMAL:
					humanMove.dontEat = !humanMove.dontEat;
					break;
				case SWT.KEYPAD_ADD:
					humanMove.open = !humanMove.open;
					break;
				case SWT.KEYPAD_1:
					humanMove.openCode = 1;
					break;
				case SWT.KEYPAD_3:
					humanMove.openCode = 2;
					break;
				case SWT.KEYPAD_MULTIPLY:
					humanMove.stopSim = !humanMove.stopSim;
					break;
				default:
					break;
				}
				
				Soar2D.wm.setStatus(human.getColor() + ": " + humanMove.toString(), black);
				
				if (go) {
					synchronized(humanMove) {
						humanMove.notify();
					}
				}
			}
		});
		
		createRHS();
		createEatersSide();

		shell.setText("Eaters");
	}
	
	public void setupKitchen() {
		worldGroup = new Group(shell, SWT.NONE);
		worldGroup.setLayout(new FillLayout());
		visualWorld = new KitchenVisualWorld(worldGroup, SWT.NONE, kKitchenMainMapCellSize);
		visualWorld.setMap(Soar2D.simulation.world.getMap());

		visualWorld.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				Player player = visualWorld.getPlayerAtPixel(new int [] { e.x, e.y });
				if (player == null) {
					return;
				}
				agentDisplay.selectPlayer(player);
			}
		});
		visualWorld.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (humanMove == null) {
					return;
				}
				boolean go = false;
				switch (e.keyCode) {
				case SWT.KEYPAD_8:
					humanMove.move = true;
					humanMove.moveDirection = Direction.NORTH;
					go = true;
					break;
				case SWT.KEYPAD_6:
					humanMove.move = true;
					humanMove.moveDirection = Direction.EAST;
					go = true;
					break;
				case SWT.KEYPAD_2:
					humanMove.move = true;
					humanMove.moveDirection = Direction.SOUTH;
					go = true;
					break;
				case SWT.KEYPAD_4:
					humanMove.move = true;
					humanMove.moveDirection = Direction.WEST;
					go = true;
					break;
				case SWT.KEYPAD_5:
					humanMove.moveWithObject = !humanMove.moveWithObject;
					break;
				case SWT.KEYPAD_1:
					humanMove.mix = true;
					go = true;
					break;
				case SWT.KEYPAD_3:
					humanMove.cook = true;
					go = true;
					break;
				case SWT.KEYPAD_0:
					humanMove.eat = true;
					go = true;
					break;

				case SWT.KEYPAD_MULTIPLY:
					humanMove.stopSim = !humanMove.stopSim;
					break;
				default:
					break;
				}
				
				Soar2D.wm.setStatus(human.getColor() + ": " + humanMove.toString(), black);
				
				if (go) {
					synchronized(humanMove) {
						humanMove.notify();
					}
				}
			}
		});
		
		createRHS();
		createKitchenSide();

		shell.setText("Kitchen");
	}
	
	public void setupTaxi() {
		worldGroup = new Group(shell, SWT.NONE);
		worldGroup.setLayout(new FillLayout());
		visualWorld = new TaxiVisualWorld(worldGroup, SWT.NONE, kTaxiMainMapCellSize);
		visualWorld.setMap(Soar2D.simulation.world.getMap());

		visualWorld.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				Player player = visualWorld.getPlayerAtPixel(new int [] { e.x, e.y });
				if (player == null) {
					return;
				}
				agentDisplay.selectPlayer(player);
			}
		});
		visualWorld.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (humanMove == null) {
					return;
				}
				switch (e.keyCode) {
				case SWT.KEYPAD_8:
					humanMove.move = true;
					humanMove.moveDirection = Direction.NORTH;
					break;
				case SWT.KEYPAD_6:
					humanMove.move = true;
					humanMove.moveDirection = Direction.EAST;
					break;
				case SWT.KEYPAD_2:
					humanMove.move = true;
					humanMove.moveDirection = Direction.SOUTH;
					break;
				case SWT.KEYPAD_4:
					humanMove.move = true;
					humanMove.moveDirection = Direction.WEST;
					break;
				case SWT.KEYPAD_5:
					humanMove.fillup = true;
					break;
				case SWT.KEYPAD_1:
					humanMove.pickup = true;
					break;
				case SWT.KEYPAD_3:
					humanMove.putdown = true;
					break;

				case SWT.KEYPAD_MULTIPLY:
					humanMove.stopSim = !humanMove.stopSim;
					break;
				default:
					break;
				}
				
				Soar2D.wm.setStatus(human.getColor() + ": " + humanMove.toString(), black);
				
				synchronized(humanMove) {
					humanMove.notify();
				}
			}
		});
		
		createRHS();
		createTaxiSide();

		shell.setText("Taxi");
	}
	
	private void createRHS() {
		rhs = new Composite(shell, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			rhs.setLayout(gl);

			GridData gd = new GridData();
			rhs.setLayoutData(gd);
		}
	}
	
	private void createEatersSide() {
		
		currentSide = new Composite(rhs, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			currentSide.setLayout(gl);
			
			GridData gd = new GridData();
			currentSide.setLayoutData(gd);
		}
		
		Group group1 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group1.setLayoutData(gd);
		}
		group1.setText("Simulation");
		group1.setLayout(new FillLayout());
		simButtons = new SimulationButtons(group1);
		
		Group group2 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group2.setLayoutData(gd);
		}
		group2.setText("Map");
		{
			GridLayout gl = new GridLayout();
			gl.numColumns = 2;
			group2.setLayout(gl);
		}
		
		Label foodLabel = new Label(group2, SWT.NONE);
		{
			GridData gd = new GridData();
			foodLabel.setLayoutData(gd);
		}
		foodLabel.setText(kFoodRemaining);
		
		foodCount = new Label(group2, SWT.NONE);
		{ 
			GridData gd = new GridData();
			gd.widthHint = 50;
			foodCount.setLayoutData(gd);
		}
		
		Label scoreLabel = new Label(group2, SWT.NONE);
		{
			GridData gd = new GridData();
			scoreLabel.setLayoutData(gd);
		}
		scoreLabel.setText(kScoreRemaining);
		
		scoreCount = new Label(group2, SWT.NONE);
		{ 
			GridData gd = new GridData();
			gd.widthHint = 50;
			scoreCount.setLayoutData(gd);
		}
		
		Label worldCountLabel = new Label(group2, SWT.NONE);
		worldCountLabel.setText(kWorldCount);
		{
			GridData gd = new GridData();
			worldCountLabel.setLayoutData(gd);
		}
		
		worldCount = new Label(group2, SWT.NONE);
		{
			GridData gd = new GridData();
			gd.widthHint = 50;
			worldCount.setLayoutData(gd);
		}
		
		updateCounts();
		
		mapButtons = new MapButtons(group2);
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			mapButtons.setLayoutData(gd);
		}

		agentDisplay = new EatersAgentDisplay(currentSide);
		{
			GridData gd = new GridData();
			agentDisplay.setLayoutData(gd);
		}
	}
	
	private void createKitchenSide() {
		
		currentSide = new Composite(rhs, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			currentSide.setLayout(gl);
			
			GridData gd = new GridData();
			currentSide.setLayoutData(gd);
		}
		
		Group group1 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group1.setLayoutData(gd);
		}
		group1.setText("Simulation");
		group1.setLayout(new FillLayout());
		simButtons = new SimulationButtons(group1);
		
		Group group2 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group2.setLayoutData(gd);
		}
		group2.setText("Map");
		{
			GridLayout gl = new GridLayout();
			gl.numColumns = 2;
			group2.setLayout(gl);
		}
		
		Label worldCountLabel = new Label(group2, SWT.NONE);
		worldCountLabel.setText(kWorldCount);
		{
			GridData gd = new GridData();
			worldCountLabel.setLayoutData(gd);
		}
		
		worldCount = new Label(group2, SWT.NONE);
		{
			GridData gd = new GridData();
			gd.widthHint = 50;
			worldCount.setLayoutData(gd);
		}
		
		updateCounts();
		
		mapButtons = new MapButtons(group2);
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			mapButtons.setLayoutData(gd);
		}

		agentDisplay = new KitchenAgentDisplay(currentSide);
		{
			GridData gd = new GridData();
			agentDisplay.setLayoutData(gd);
		}
	}
	
	private void createTaxiSide() {
		
		currentSide = new Composite(rhs, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			currentSide.setLayout(gl);
			
			GridData gd = new GridData();
			currentSide.setLayoutData(gd);
		}
		
		Group group1 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group1.setLayoutData(gd);
		}
		group1.setText("Simulation");
		group1.setLayout(new FillLayout());
		simButtons = new SimulationButtons(group1);
		
		Group group2 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group2.setLayoutData(gd);
		}
		group2.setText("Map");
		{
			GridLayout gl = new GridLayout();
			gl.numColumns = 2;
			group2.setLayout(gl);
		}
		
		Label worldCountLabel = new Label(group2, SWT.NONE);
		worldCountLabel.setText(kWorldCount);
		{
			GridData gd = new GridData();
			worldCountLabel.setLayoutData(gd);
		}
		
		worldCount = new Label(group2, SWT.NONE);
		{
			GridData gd = new GridData();
			gd.widthHint = 50;
			worldCount.setLayoutData(gd);
		}
		
		updateCounts();
		
		mapButtons = new MapButtons(group2);
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			mapButtons.setLayoutData(gd);
		}

		agentDisplay = new TaxiAgentDisplay(currentSide);
		{
			GridData gd = new GridData();
			agentDisplay.setLayoutData(gd);
		}
	}
	
	private void createBookSide() {
		
		currentSide = new Composite(rhs, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			currentSide.setLayout(gl);
			
			GridData gd = new GridData();
			currentSide.setLayoutData(gd);
		}
		
		Group group1 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group1.setLayoutData(gd);
		}
		group1.setText("Simulation");
		group1.setLayout(new FillLayout());
		simButtons = new SimulationButtons(group1);
		
		Group group2 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group2.setLayoutData(gd);
		}
		group2.setText("Map");
		{
			GridLayout gl = new GridLayout();
			gl.numColumns = 2;
			group2.setLayout(gl);
		}
		
		Label worldCountLabel = new Label(group2, SWT.NONE);
		worldCountLabel.setText(kWorldCount);
		{
			GridData gd = new GridData();
			worldCountLabel.setLayoutData(gd);
		}
		
		worldCount = new Label(group2, SWT.NONE);
		{
			GridData gd = new GridData();
			gd.widthHint = 50;
			worldCount.setLayoutData(gd);
		}
		
		updateCounts();
		
		mapButtons = new MapButtons(group2);
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			mapButtons.setLayoutData(gd);
		}

		agentDisplay = new BookAgentDisplay(currentSide);
		{
			GridData gd = new GridData();
			agentDisplay.setLayoutData(gd);
		}
	}
	
	public class GetIdDialog extends Dialog {
		public GetIdDialog(Shell parent) {
			super(parent);
		}
		
		public void open() {
			Shell parent = getParent();
			final Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			dialog.setText("Get ID");
			
			GridLayout gl = new GridLayout();
			gl.numColumns = 2;
			dialog.setLayout(gl);

			final Label label2 = new Label(dialog, SWT.NONE);
			{
				GridData gd = new GridData();
				gd.horizontalAlignment = GridData.BEGINNING;
				label2.setLayoutData(gd);
			}
			label2.setText("Id:");
			
			final Button ok = new Button(dialog, SWT.PUSH);
			{
				GridData gd = new GridData();
				gd.horizontalAlignment = GridData.BEGINNING;
				ok.setLayoutData(gd);
			}
			ok.setText("Ok");
			ok.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					dialog.dispose();
				}
			});

			dialog.setSize(dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT, false));
			
			dialog.open();
			Display display = parent.getDisplay();
			while (!dialog.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
	}
	
	public void setupBook() {
		worldGroup = new Group(shell, SWT.NONE);
		worldGroup.setLayout(new FillLayout());
		visualWorld = new BookVisualWorld(worldGroup, SWT.NONE, Soar2D.config.roomConfig().cell_size);
		visualWorld.setMap(Soar2D.simulation.world.getMap());

		visualWorld.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				Player player = visualWorld.getPlayerAtPixel(new int [] { e.x, e.y });
				if (player == null) {
					return;
				}
				agentDisplay.selectPlayer(player);
			}
		});
		visualWorld.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (humanMove == null) {
					return;
				}
				boolean go = false;
				switch (e.keyCode) {
				case SWT.KEYPAD_9:
					GetIdDialog getDialog = new GetIdDialog(e.display.getShells()[0]);
					getDialog.open();
					break;
				case SWT.KEYPAD_8:
					humanMove.forward = true;
					break;
				case SWT.KEYPAD_4:
					humanMove.rotate = true;
					humanMove.rotateDirection = Names.kRotateLeft;
					break;
				case SWT.KEYPAD_5:
					humanMove.rotate = true;
					humanMove.rotateDirection = Names.kRotateStop;
					break;
				case SWT.KEYPAD_6:
					humanMove.rotate = true;
					humanMove.rotateDirection = Names.kRotateRight;
					break;
				case SWT.KEYPAD_3:
					humanMove.drop = true;
					humanMove.dropId = human.getCarryId();
					break;
				case SWT.KEYPAD_2:
					humanMove.backward = true;
					break;
				case SWT.KEYPAD_MULTIPLY:
					humanMove.stopSim = true;
					break;
				case SWT.KEYPAD_CR:
					go = true;
					break;
				default:
					break;
				}
				
				Soar2D.wm.setStatus(human.getColor() + ": " + humanMove.toString(), black);
				
				if (go) {
					synchronized(humanMove) {
						humanMove.notify();
					}
				}
			}
		});
		
		createRHS();
		createBookSide();

		shell.setText("Book");
	
	}
	public void setupTankSoar() {
		worldGroup = new Group(shell, SWT.NONE);
		worldGroup.setLayout(new FillLayout());
		visualWorld = new TankSoarVisualWorld(worldGroup, SWT.NONE, kTanksoarMainMapCellSize);
		visualWorld.setMap(Soar2D.simulation.world.getMap());
		
		visualWorld.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (humanMove == null) {
					return;
				}
				boolean go = false;
				Direction facing = human.getFacing();
				switch (e.keyCode) {
				case SWT.KEYPAD_8:
					humanMove.move = true;
					humanMove.moveDirection = facing;
					break;
				case SWT.KEYPAD_6:
					humanMove.move = true;
					humanMove.moveDirection = facing.right();
					break;
				case SWT.KEYPAD_2:
					humanMove.move = true;
					humanMove.moveDirection = facing.backward();
					break;
				case SWT.KEYPAD_4:
					humanMove.move = true;
					humanMove.moveDirection = facing.left();
					break;
				case SWT.KEYPAD_1:
					humanMove.rotate = !humanMove.rotate;
					humanMove.rotateDirection = Names.kRotateLeft;
					break;
				case SWT.KEYPAD_3:
					humanMove.rotate = !humanMove.rotate;
					humanMove.rotateDirection = Names.kRotateRight;
					break;
				case SWT.KEYPAD_5:
					humanMove.move = false;
					break;
				case SWT.KEYPAD_0:
					humanMove.fire = !humanMove.fire;
					break;
				case SWT.KEYPAD_7:
					humanMove.shields = !humanMove.shields;
					humanMove.shieldsSetting = true;
					break;
				case SWT.KEYPAD_9:
					humanMove.shields = !humanMove.shields;
					humanMove.shieldsSetting = false;
					break;
				case SWT.KEYPAD_MULTIPLY:
					humanMove.stopSim = !humanMove.stopSim;
					break;
				case SWT.KEYPAD_ADD:
					humanMove.radar = true;
					humanMove.radarSwitch = true;
					humanMove.radarPower = true;
					if (humanMove.radarPowerSetting <= 0) {
						humanMove.radarPowerSetting = 1;
					} else {
						humanMove.radarPowerSetting += 1;
					}
					break;
				case SWT.KEYPAD_SUBTRACT:
					humanMove.radar = true;
					humanMove.radarPower = true;
					humanMove.radarPowerSetting -= 1;
					if (humanMove.radarPowerSetting <= 0) {
						humanMove.radarPowerSetting = 0;
						humanMove.radarSwitch = false;
					} else {
						humanMove.radarSwitch = true;
					}
					break;
				case SWT.KEYPAD_CR:
					go = true;
					break;
				default:
					break;
				}
				
				Soar2D.wm.setStatus(human.getColor() + ": " + humanMove.toString(), black);
				
				if (go) {
					synchronized(humanMove) {
						humanMove.notify();
					}
				}
			}
		});
		
		createRHS();
		createTankSoarSide();

		shell.setText("TankSoar");
	}
	
	private void createTankSoarSide() {
		currentSide = new Composite(rhs, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			gl.numColumns = 2;
			currentSide.setLayout(gl);
			
			GridData gd = new GridData();
			currentSide.setLayoutData(gd);
		}
		
		Group group1 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group1.setLayoutData(gd);
		}
		group1.setText("Simulation");
		group1.setLayout(new FillLayout());
		simButtons = new SimulationButtons(group1);
		
		Group group2 = new Group(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			group2.setLayoutData(gd);
		}
		group2.setText("Map");
		group2.setLayout(new FillLayout());
		mapButtons = new MapButtons(group2);

		Composite comp1 = new Composite(currentSide, SWT.NONE);
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			comp1.setLayoutData(gd);
		}
		{
			GridLayout gl = new GridLayout();
			gl.numColumns = 2;
			comp1.setLayout(gl);
		}

		Label worldCountLabel = new Label(comp1, SWT.NONE);
		worldCountLabel.setText(kWorldCount);
		{
			GridData gd = new GridData();
			worldCountLabel.setLayoutData(gd);
		}
		
		worldCount = new Label(comp1, SWT.NONE);
		{ 
			GridData gd = new GridData();
			gd.widthHint = 50;
			worldCount.setLayoutData(gd);
		}
		
		updateCounts();
		
		agentDisplay = new TankSoarAgentDisplay(currentSide);
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			agentDisplay.setLayoutData(gd);
		}
	}
	
	void processEdit(int [] location) {
		if (templatesTable.getSelectionIndex() < 0) {
			return;
		}
		
		if (Soar2D.config.game() == SimConfig.Game.TANKSOAR) {
			this.editMap.removeAll(location);
		}

		String newContent = null;
		
		if (templatesTable.getSelectionIndex() == 0) {
			// clear out the cell
			this.editMap.removeAll(location);
			
			if (Soar2D.config.game() == SimConfig.Game.TANKSOAR) {
				newContent = Names.kGround;
			}
		} else {
			newContent = templatesTable.getSelection()[0].getText();
		}

		if (newContent != null) {
			this.editMap.addObjectToCell(location, this.editMap.createObjectByName(newContent));
		}

		if (Soar2D.config.game() == SimConfig.Game.TANKSOAR) {
			TankSoarVisualWorld tsVisualWorld = (TankSoarVisualWorld)visualWorld;
			tsVisualWorld.updateBackground(location);
		}
		visualWorld.redraw();
		return;
	}
	
	Table templatesTable;

	private void createEditSide() {
		currentSide = new Composite(rhs, SWT.NONE);
		{
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			currentSide.setLayout(gl);
			
			GridData gd = new GridData();
			currentSide.setLayoutData(gd);
		}
		
		Label templatesLabel = new Label(currentSide, SWT.NONE);
		templatesLabel.setText("Templates:");
		
		templatesTable = new Table(currentSide, SWT.BORDER | SWT.V_SCROLL);
		{
			TableItem item = new TableItem(templatesTable, SWT.NONE);
			item.setText("<empty>");
		}
		Iterator<CellObject> templateIter = Soar2D.simulation.world.getMap().getObjectManager().getTemplates().iterator();
		while(templateIter.hasNext()) {
			CellObject template = templateIter.next();
			String templateName = template.getName();
			
			if (templateName.equals(Names.kPropertyMissile)
					|| templateName.equals(Names.kExplosion)
					|| templateName.equals(Names.kGround)
					|| templateName.equals(Names.kMissiles)) {
				continue;
			}
			
			TableItem item = new TableItem(templatesTable, SWT.NONE);
			item.setText(template.getName());
		}
		
//		Button editTemplateButton = new Button(currentSide, SWT.PUSH);
//		editTemplateButton.setText("Edit Selected Template");
//
//		Button destroyTemplateButton = new Button(currentSide, SWT.PUSH);
//		destroyTemplateButton.setText("Destroy Selected Template");
//
//		Button newTemplateButton = new Button(currentSide, SWT.PUSH);
//		newTemplateButton.setText("Create New Template");
		
		if (Soar2D.config.game() == SimConfig.Game.EATERS) {
			final Button randomFoodButton = new Button(currentSide, SWT.CHECK);
			randomFoodButton.setText("Random food");
			randomFoodButton.setSelection(Soar2D.simulation.world.getMap().getRandomFood());
			randomFoodButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Soar2D.simulation.world.getMap().setRandomFood(randomFoodButton.getSelection());
				}
			});
			
			final Button randomWallsButton = new Button(currentSide, SWT.CHECK);
			randomWallsButton.setText("Random walls");
			randomWallsButton.setSelection(Soar2D.simulation.world.getMap().getRandomWalls());
			randomWallsButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Soar2D.simulation.world.getMap().setRandomWalls(randomWallsButton.getSelection());
				}
			});
		}
		
		Button saveAsButton = new Button(currentSide, SWT.PUSH);
		saveAsButton.setText("Save as...");
		saveAsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				exitEditMode(true);
			}
		});
		
		Button cancelButton = new Button(currentSide, SWT.PUSH);
		cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				exitEditMode(false);
			}
		});

	}
	
	Menu menuBar;
	Menu fileMenu;
	Menu mapMenu;
	Menu helpMenu;
	
	MenuItem fileMenuHeader;
	MenuItem mapMenuHeader;
	MenuItem helpMenuHeader;
	
	//MenuItem fileConfigurationItem;
	MenuItem fileExitItem;
	
	MenuItem mapChangeItem;
	MenuItem mapEditItem;
	
	MenuItem helpAboutItem;

	boolean mapEditMode = false;

	private GridMap editMap;
	
	private void exitEditMode(boolean save) {
		assert mapEditMode == true;

		// we're going in to normal mode, destroy current side, create normal side
		assert this.editMap != null;

		if (save) {
	   		File mapFile = saveMap();
			if (mapFile == null) {
				return;
			}
			Soar2D.config.generalConfig().map = mapFile.getAbsolutePath();
		}
		
   		logger.info("Exiting map editor.");
   		mapEditMode = false;
   		
   		mapMenuHeader.setEnabled(true);

 		Soar2D.simulation.world.load();
		this.editMap = null;
		
		currentSide.dispose();
		switch (Soar2D.config.game()) {
		case EATERS:
			createEatersSide();
			break;
		case TANKSOAR:
    		createTankSoarSide();
    		break;
		case ROOM:
    		createBookSide();
    		break;
		case KITCHEN:
			createKitchenSide();
			break;
		case TAXI:
			createTaxiSide();
			break;
		}
		
		this.reset();
		
		rhs.layout(true);
		shell.layout(true);
	}
	
	private void enterEditMode() {
		assert mapEditMode == false;
		
		if (Soar2D.simulation.world.getPlayers().numberOfPlayers() > 0) {
			Soar2D.control.infoPopUp("Destroy all agents before editing the map.");
			return;
		}
		
		// flip the bit
    	mapEditMode = true;
    	
		// we're going in to edit mode, destroy current side, create edit side

		logger.info("Entering map editor.");
	
		mapMenuHeader.setEnabled(false);
		
		switch (Soar2D.config.game()) {
		case EATERS:
			this.editMap = new EatersMap();
			break;
		case TANKSOAR:
			this.editMap = new TankSoarMap();
			break;
		case ROOM:
			this.editMap = new BookMap();
			break;
		case KITCHEN:
			this.editMap = new KitchenMap();
			break;
		case TAXI:
			this.editMap = new TaxiMap();
			break;
		}
		
		try {
			this.editMap.load();
		} catch (GridMap.LoadError e) {
			// TODO: handle gracefully
			assert false;
		}
		this.visualWorld.setMap(this.editMap);
		
		currentSide.dispose();
		createEditSide();
		
		updateWorldGroup();
		
		rhs.layout(true);
		shell.layout(true);

	}
	
	private void error(String message) {
		logger.error(message);
		if (using()) {
			errorMessage(Soar2D.config.title(), message);
		}
	}
	
	private File saveMap() {
		String output = this.editMap.generateXMLString();

		if (output == null) {
			error("Couldn't generate map file.");
			return null;
		}
		
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setText("Map must be saved to continue...");
		fd.setFilterPath(Soar2D.simulation.getMapPath());
		
		fd.setFilterExtensions(new String[] {"*." + Soar2D.simulation.getMapExt(), "*.*"});
		
		String mapFileString = fd.open();
		if (mapFileString != null) {
			if (!mapFileString.matches(".*\\..+")) {
				mapFileString += "." + Soar2D.simulation.getMapExt();
			}
			
			File mapFile = new File(mapFileString);
			if (mapFile.exists() && !mapFile.canWrite()) {
				error("Cannot write to file.");
				return null;
			}
			try {
				FileWriter out = new FileWriter(mapFile);
				out.write(output);
				out.close();
			} catch (IOException exception) {
				error("Error writing file: " + exception.getMessage());
				return null;
			}
			return mapFile;
		}
		return null;
	}
	
	public void run() {
		
		menuBar = new Menu(shell, SWT.BAR);
		fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");
		
		fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);
		
//		fileConfigurationItem = new MenuItem(fileMenu, SWT.PUSH);
//		fileConfigurationItem.setText("&Configuration");
//		fileConfigurationItem.addSelectionListener(new SelectionListener() {
//		    public void widgetSelected(SelectionEvent event) {
//		    	ConfigurationEditor ce = new ConfigurationEditor(shell);
//		    	ce.open();
//		    }
//			
//			public void widgetDefaultSelected(SelectionEvent event) {
//		    	ConfigurationEditor ce = new ConfigurationEditor(shell);
//		    	ce.open();
//			}
//		});

		fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText("&Exit");
		fileExitItem.addSelectionListener(new SelectionListener() {
		    public void widgetSelected(SelectionEvent event) {
		        shell.close();
		        display.dispose();
		    }

		    public void widgetDefaultSelected(SelectionEvent event) {
		        shell.close();
		        display.dispose();
		    }
		});
		
		mapMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		mapMenuHeader.setText("&Map");
		
		mapMenu = new Menu(shell, SWT.DROP_DOWN);
		mapMenuHeader.setMenu(mapMenu);
		
		mapChangeItem = new MenuItem(mapMenu, SWT.PUSH);
		mapChangeItem.setText("&Change Map");
		mapChangeItem.addSelectionListener(new SelectionListener() {
		    public void widgetSelected(SelectionEvent event) {
		    	mapButtons.changeMap();
		    }

		    public void widgetDefaultSelected(SelectionEvent event) {
		    	mapButtons.changeMap();
		    }
		});

		mapEditItem = new MenuItem(mapMenu, SWT.PUSH);
		mapEditItem.setText("&Edit Map");
		mapEditItem.addSelectionListener(new SelectionListener() {
		    public void widgetSelected(SelectionEvent event) {
		    	enterEditMode();
		    }

		    public void widgetDefaultSelected(SelectionEvent event) {
		    	enterEditMode();
		    }
		});

		helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&Help");

		helpMenu = new Menu(shell, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);
		
		helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
		helpAboutItem.setText("&About");
		helpAboutItem.addSelectionListener(new SelectionListener() {
		    public void widgetSelected(SelectionEvent event) {
		        infoMessage("About", "Soar2D\nby Jonathan Voigt\nvoigtjr@gmail.com");
			}
			
			public void widgetDefaultSelected(SelectionEvent event) {
				infoMessage("About", "Soar2D\nby Jonathan Voigt\nvoigtjr@gmail.com");
			}
		});
		
		shell.setMenuBar(menuBar);
		
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		shell.setLayout(gl);

		switch (Soar2D.config.game()) {
		case EATERS:
			setupEaters();
			break;
		case TANKSOAR:
			setupTankSoar();
			break;
			
		case ROOM:
			setupBook();
			break;
			
		case KITCHEN:
			setupKitchen();
			break;
			
		case TAXI:
			setupTaxi();
			break;
		}
		
		statusLine = new Label(shell, SWT.BORDER);
		statusLine.setText("Ready");
		{
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.FILL;
			gd.heightHint = 16;
			statusLine.setLayoutData(gd);
		}
		
		visualWorld.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (mapEditMode) {
					int [] location = visualWorld.getCellAtPixel(new int [] { e.x, e.y });
					processEdit(location);
				} else {
					Player player = visualWorld.getPlayerAtPixel(new int [] { e.x, e.y });
					if (player == null) {
						return;
					}
					agentDisplay.selectPlayer(player);
				}
			}
		});
		
		updateWorldGroup();

		VisualWorld.remapPlayerColors();

		shell.addShellListener(new ShellAdapter() {
			public void shellDeactivated(ShellEvent e) {
				agentDisplay.worldChangeEvent();			
				visualWorld.redraw();			
			}
			public void shellActivated(ShellEvent e) {
				agentDisplay.worldChangeEvent();			
				visualWorld.setRepaint();
				visualWorld.redraw();			
			}
			public void shellDeiconified(ShellEvent e) {
				agentDisplay.worldChangeEvent();			
				visualWorld.setRepaint();
				visualWorld.redraw();			
			}
		});
		
		shell.addControlListener(new ControlAdapter() {
			public void controlMoved(ControlEvent e) {
				agentDisplay.worldChangeEvent();			
				visualWorld.setRepaint();
				visualWorld.redraw();			
			}
			public void controlResized(ControlEvent e) {
				agentDisplay.worldChangeEvent();			
				visualWorld.setRepaint();
				visualWorld.redraw();			
			}
		});

		int [] xy = Soar2D.config.getWindowPosition();
		if (xy != null && xy.length == 2)
		{
			// it shouldn't be created off-screen
			if ((xy[0] < display.getClientArea().width - 100) && (xy[1] < display.getClientArea().height - 100)) {
				shell.setLocation(xy[0], xy[1]);
			} else {
				logger.warn("Ignoring old window location, screen is too small.");
			}
		}

		rhs.layout(true);
		shell.layout(true);
		
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	public void infoMessage(String title, String message) {
		popUpTitle = title;
		popUpMessage = message;
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.WRAP);
					mb.setMessage(popUpMessage);
					mb.setText(popUpTitle);
					mb.open();
				}
			});
		}
	}

	public void errorMessage(String title, String message) {
		popUpTitle = title;
		popUpMessage = message;
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.WRAP);
					mb.setMessage(popUpMessage);
					mb.setText(popUpTitle);
					mb.open();
				}
			});
		}
	}
	
	public void setStatus(String status, final Color color) {
		if (!using()) {
			return;
		}
		statusMessage = status;
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					statusLine.setText(statusMessage);
					if (color != null) {
						statusLine.setForeground(color);
					}
				}
			});
		}
	}

	void updateWorldGroup() {
		worldGroup.setText("Map: " + Soar2D.config.generalConfig().map);
		visualWorld.setSize(visualWorld.getWidth(), visualWorld.getHeight());
		GridData gd = new GridData();
		gd.widthHint = visualWorld.getWidth();
		gd.heightHint = visualWorld.getHeight();
		gd.verticalSpan = 3;
		worldGroup.setLayoutData(gd);

		shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	void updateCounts() {
		if (Soar2D.config.game() == SimConfig.Game.EATERS) {
			EatersMap eMap = (EatersMap)Soar2D.simulation.world.getMap();
			foodCount.setText(Integer.toString(eMap.getFoodCount()));
			scoreCount.setText(Integer.toString(eMap.getScoreCount()));
		}
		worldCount.setText(Integer.toString(Soar2D.simulation.world.getWorldCount()));
	}

	boolean isDisposed() {
		return (display.isDisposed() || shell.isDisposed());
	}

	public void update() {
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					agentDisplay.worldChangeEvent();
					visualWorld.redraw();
					updateCounts();
				}
			});
		}
	}

	public void start() {
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					simButtons.updateButtons();
					mapButtons.updateButtons();
					agentDisplay.updateButtons();
				}
			});
		}
	}

	public void stop() {
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					visualWorld.setRepaint();
					visualWorld.redraw();
					simButtons.updateButtons();
					mapButtons.updateButtons();
					agentDisplay.updateButtons();
				}
			});
		}
	}
	
	public void playerEvent() {
		if (visualWorld != null && !visualWorld.isDisposed()) {
			visualWorld.setRepaint();
			visualWorld.redraw();
			VisualWorld.remapPlayerColors();
			updateCounts();
			simButtons.updateButtons();
			agentDisplay.agentEvent();
		}
	}
	
	public void reset() {
		if (!isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					visualWorld.setMap(Soar2D.simulation.world.getMap());
					agentDisplay.setMap(Soar2D.simulation.world.getMap());
					updateWorldGroup();
					agentDisplay.worldChangeEvent();
					visualWorld.redraw();
					updateCounts();
					simButtons.updateButtons();
				}
			});
		}
	}

	public void shutdown() {

		if (humanMove != null) {
			synchronized(humanMove) {
				humanMove.notifyAll();
			}
		}
		
		if (display == null) {
			shell = null;
			return;
		}
		
		if (!display.isDisposed()) {
			display.dispose();
		}
		
		display = null;
		shell = null;
	}
	
	void setVisualWorldFocus() {
		display.syncExec(new Runnable() {
			public void run() {
				visualWorld.setFocus();
			}
		});
	}

	public MoveInfo getHumanMove(Player player) {
		humanMove = new MoveInfo();
		if (Soar2D.config.generalConfig().headless) {
			return humanMove;
		}
		if (player.getRadarSwitch()) {
			humanMove.radar = true;
			humanMove.radarSwitch = true;
		}
		if (player.getRadarPower() > 0) {
			humanMove.radarPower = true;
			humanMove.radarPowerSetting = player.getRadarPower();
		}
		if (!isDisposed()) {
			human = player;
			if (player == null) {
				return null;
			}
			setStatus("Enter move for " + player.getColor(), WindowManager.getColor(player.getColor()));
			setVisualWorldFocus();
			synchronized(humanMove) {
				try {
					humanMove.wait();
				} catch (InterruptedException e) {}
			}
		}
		MoveInfo theMove = humanMove;
		humanMove = null;
		return theMove;
	}

	public String promptForConfig() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
		fd.setText("Select configuration file");
		fd.setFilterPath(System.getProperty("user.dir") + System.getProperty("file.separator"));
		fd.setFileName(Names.configs.eatersCnf);
		fd.setFilterExtensions(new String[] {"*.cnf", "*.*"});
		return fd.open();
	}
}