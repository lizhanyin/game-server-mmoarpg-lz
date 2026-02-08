@echo off
echo 开始提取消息定义...

echo === SCSellFrag ===
sed -n "150805,152348p" "d:\work\Java\game-server-mmoarpg-lz\lzWorldSrv\gen\org\gof\demo\worldsrv\msg\Msg.java.bak" | grep -E "^// (required|optional|repeated).*= [0-9]+;" | sort | uniq

echo.
echo === SCFragInfo ===
sed -n "148398,150292p" "d:\work\Java\game-server-mmoarpg-lz\lzWorldSrv\gen\org\gof\demo\worldsrv\msg\Msg.java.bak" | grep -E "^// (required|optional|repeated).*= [0-9]+;" | sort | uniq

echo.
echo === SCOneFragInfo ===
sed -n "149737,150292p" "d:\work\Java\game-server-mmoarpg-lz\lzWorldSrv\gen\org\gof\demo\worldsrv\msg\Msg.java.bak" | grep -E "^// (required|optional|repeated).*= [0-9]+;" | sort | uniq

echo.
echo === SCGeneralList ===
sed -n "140750,141889p" "d:\work\Java\game-server-mmoarpg-lz\lzWorldSrv\gen\org\gof\demo\worldsrv\msg\Msg.java.bak" | grep -E "^// (required|optional|repeated).*= [0-9]+;" | sort | uniq
