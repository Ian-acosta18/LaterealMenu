package com.example.laterealmenu;

import  android.content.Context;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Adaptador extends BaseAdapter {
    private Context context;
    private String[] nombreElementos;
    private int[] elementoImagen;
    private LayoutInflater inflater;

    public Adaptador(Context contexto, String[] nombreElementos, int[] elementoImagen){
        this.context = contexto;
        this.nombreElementos = nombreElementos;
        this.elementoImagen = elementoImagen;
        inflater = LayoutInflater.from(contexto);
    }

    @Override
    public int getCount() {
        return nombreElementos.length;
    }

    @Override
    public Object getItem(int i) {
        return nombreElementos[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if(view == null){
            view = inflater.inflate(R.layout.elementos_lista, parent, false);
        }
        ImageView imageView = view.findViewById(R.id.imageView3);
        TextView textView = view.findViewById(R.id.textView2);

        textView.setText(nombreElementos[i]);
        imageView.setImageResource(elementoImagen[i]);
        return view;
    }
}

