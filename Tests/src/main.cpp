#include "portability.h"

#include <cppunit/TestRunner.h>
#include <cppunit/TestResult.h>
#include <cppunit/TestResultCollector.h>
#include <cppunit/extensions/HelperMacros.h>
#include <cppunit/BriefTestProgressListener.h>
#include <cppunit/extensions/TestFactoryRegistry.h>
#include <cppunit/CompilerOutputter.h>
#include <ctime>

#include "simplelistener.h"

bool g_Cancel = false;

#ifdef _WIN32
BOOL WINAPI handle_ctrlc( DWORD dwCtrlType )
{
	if ( dwCtrlType == CTRL_C_EVENT )
	{
		g_Cancel = true;
		return TRUE;
	}

	return FALSE;
}
#endif // _WIN32

int main( int argc, char** argv )
{
#ifdef _WIN32
	//_crtBreakAlloc = 2168;
	_CrtSetDbgFlag ( _CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF ); 
	SetConsoleCtrlHandler( handle_ctrlc, TRUE );
#endif // _WIN32

	bool pause = true;
	if ( argc >= 2 )
	{
		if ( std::string( argv[1] ) == "--listener" ) 
		{
			int port = 12121;
			if ( argc >= 3 )
			{
				port = atoi( argv[2] );
			}
			SimpleListener simpleListener( 600, port );
			return simpleListener.run();
		}
		if ( std::string( argv[1] ) == "--nopause" ) pause = false;
	}

	srand( static_cast<unsigned>( time( NULL ) ) );

	//--- Create the event manager and test controller
	CPPUNIT_NS::TestResult controller;

	//--- Add a listener that colllects test result
	CPPUNIT_NS::TestResultCollector result;
	controller.addListener( &result );        

	//--- Add a listener that print dots as test run.
	CPPUNIT_NS::BriefTestProgressListener progress;
	controller.addListener( &progress );      

	//--- Add the top suite to the test runner
	CPPUNIT_NS::TestRunner runner;
	runner.addTest( CPPUNIT_NS::TestFactoryRegistry::getRegistry().makeTest() );
	runner.run( controller );

	CPPUNIT_NS::CompilerOutputter outputter( &result, std::cerr );
	outputter.write();                      

	if ( pause )
	{
		std::cout << std::endl << "Strange errors? Make sure working directory is 'SoarLibrary/bin'."
			<< std::endl << "Press enter to exit." << std::endl;
		std::cin.get();
	}

	return result.wasSuccessful() ? 0 : 1;
}