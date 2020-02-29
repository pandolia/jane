switch ($args[0]) {
    'resolve' {
        mvn dependency:resolve
        return
    }

    'clean' {
        mvn clean
        return
    }

    'compile' {
        mvn compile
        return
    }

    'package' {
        mvn package
        Move-Item '.\target\jane-1.0.0-jar-with-dependencies.jar' '.\bin\jane.1.0.0.jar' -Force
        return
    }

    # make a container with Docker Toolbox for Windows
    # must add share folder to the virtual linux system in Virtual box
    'make-container' {
        docker container run `
            --name jane `
            --publish 8000:8000 `
            --volume /d/Kotlin/jane/jane/bin:/home/root/jane `
            --workdir /home/root/jane `
            --interactive `
            --tty `
            adoptopenjdk:8-jre-openj9
        return
    }

    # run jane dev in the container, the web site is: http://192.168.99.100:8000/
    'bash' {
        docker start jane --interactive
        return
    }

    default {
        Write-Host "Bad command: $op"
    }
}