rdom () { local IFS=\> ; read -d \< E C ;}

curl -sS www.user-agents.org/allagents.xml | while rdom; do
    if [[ $E = String ]]; then
        echo $C
    fi
    #if [[ $E = Description ]]; then
    #    echo 'Description:'$C
    #fi
done
