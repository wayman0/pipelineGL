set joglJar=^
../joglLIB/gluegen-rt.jar;^
../joglLIB/gluegen-rt-natives-windows-amd64.jar;^
../joglLIB/jogl-all.jar;^
../joglLIB/jogl-all-natives-windows-amd64.jar;

javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                                  renderer\framebuffer\*.java  &&^
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                                        renderer\scene\*.java  &&^
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                             renderer\scene\primitives\*.java  &&^
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                                   renderer\scene\util\*.java  &&^
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                                     renderer\models_L\*.java  &&^
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                      renderer\models_L\turtlegraphics\*.java  &&^
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                                     renderer\pipeline\*.java 

pause
cls 
javac -g -Xlint -Xdiags:verbose -encoding Windows-1252 -cp .;..;%joglJar%;               renderer\pipelineGL\*.java &&^


javac -g -Xlint -Xdiags:verbose -encoding Windows-1252                                            clients_r1\*.java
pause
