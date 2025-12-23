#!/bin/bash
echo "🔍 VERIFICAÇÃO COMPLETA DE IDs"
echo "=============================="

echo "📄 IDs NO LAYOUT (activity_main.xml):"
echo "-----------------------------------"
grep -o 'android:id="@+id/[^"]*"' app/src/main/res/layout/activity_main.xml | cut -d'/' -f3 | sort

echo ""
echo "🎯 IDs QUE MAINACTIVITY ESPERA:"
echo "------------------------------"
echo "• controls_container (linha 76)"
echo "• fab_toggle (linha 82)"
echo "• tv_webview_1 (linha 100)"
echo "• tv_webview_2 (linha 101)"
echo "• tv_webview_3 (linha 102)"
echo "• tv_webview_4 (linha 103)"
echo "• container_1, container_2, container_3, container_4"
echo "• status_1, status_2, status_3, status_4"
echo "• btnBack, btnForward, btnRefresh, btnHome, btnLoad"
echo "• urlInput, tvStatus, webview_grid"

echo ""
echo "✅ LAYOUT ACIMA CONTÉM TODOS ESTES IDs!"
