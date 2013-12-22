sh clear.sh 
cp *.properties bin/ 
java -jar bin/ApacheJmeter.jar -t TestPlan/Concurrent/CT80Update_test.jmx
